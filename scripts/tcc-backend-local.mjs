import { createRequire } from 'node:module';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const backend = process.argv[2];
const operador = 'operador.tcc@example.invalid';
if (!backend || process.env.DB_HOST !== '127.0.0.1' ||
    process.env.DB_NAME !== 'db_watt_vision' || process.env.DEMO_MODE !== 'false' ||
    process.env.JWT_SECRET?.length < 32 || !process.env.JWT_SECRET ||
    !/^[0-9]+$/.test(process.env.DB_PORT ?? '') || !process.env.DB_USER || !process.env.DB_PASSWORD) {
    throw new Error('Configure o Backend validado, a instância descartável local, a conta SQL e o segredo JWT.');
}

const pasta = resolve(backend);
const carregar = createRequire(resolve(pasta, 'package.json'));
const express = carregar('express');
const importar = async (arquivo) => (await import(pathToFileURL(resolve(pasta, arquivo)))).default;
const banco = await importar('src/config/database.js');
const [escopo] = await banco.execute(`
    SELECT e.id, l.id AS id_local
    FROM tbl_empresa e
    JOIN tbl_local l ON l.id_empresa = e.id
    JOIN tbl_usuario u ON u.id_empresa = e.id
    JOIN tbl_usuario_local ul ON ul.id_usuario = u.id AND ul.id_local = l.id
    WHERE e.cnpj = '00.000.000/0000-00' AND l.codigo = 'TCC-LOC-01'
      AND u.email = ? AND u.perfil = 'Gestor' AND u.status = 'Ativo'
      AND (SELECT COUNT(*) FROM tbl_empresa) = 1
      AND (SELECT COUNT(*) FROM tbl_local) = 1
      AND (SELECT COUNT(*) FROM tbl_usuario) = 1
`, [operador]);
if (escopo.length !== 1) {
    await banco.end();
    throw new Error('A base local não corresponde ao escopo sintético do operador TCC.');
}

const [api, autenticar] = await Promise.all([
    importar('src/app.js'),
    importar('src/middlewares/authMiddleware.js')
]);
const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '16kb' }));

const listas = new Set([
    '/api/locais', '/api/setores', '/api/equipamentos', '/api/pontos-medicao',
    '/api/pzems', '/api/alertas', '/api/dispositivos-iot'
]);
const detalhe = /^\/api\/(?:equipamentos|limites)\/[1-9][0-9]*$/;
const inteiro = (valor) => typeof valor === 'string' && /^[1-9][0-9]*$/.test(valor) &&
    Number.isSafeInteger(Number(valor));

app.use((req, res, next) => {
    const caminho = req.path;
    const chaves = Object.keys(req.query);
    if (req.method === 'GET' && chaves.length === 0 && (caminho === '/' || caminho === '/api/auth/me')) {
        return next();
    }
    if (req.method === 'POST' && caminho === '/api/auth/login' && chaves.length === 0) {
        if (req.body?.email === operador) return next();
        return res.status(403).json({ sucesso: false, mensagem: 'Conta fora do escopo do ensaio.' });
    }

    const serie = caminho === '/api/medicoes/serie-temporal' && chaves.length === 2 &&
        inteiro(req.query.id_pzem) && inteiro(req.query.horas) && Number(req.query.horas) <= 720;
    const leitura = chaves.length === 0 && (listas.has(caminho) || detalhe.test(caminho));
    if (req.method !== 'GET' || (!serie && !leitura)) {
        return res.status(404).json({ sucesso: false, mensagem: 'Rota indisponível no ensaio.' });
    }

    return autenticar(req, res, () => {
        const usuario = req.usuario;
        if (usuario.email !== operador || usuario.id_empresa !== escopo[0].id || usuario.perfil !== 'Gestor') {
            return res.status(403).json({ sucesso: false, mensagem: 'Conta fora do escopo do ensaio.' });
        }
        // A consulta original usa SELECT *. Entregue somente os campos de conectividade usados no app.
        if (caminho === '/api/dispositivos-iot') {
            const json = res.json.bind(res);
            res.json = (corpo) => json(Array.isArray(corpo.dados) ? {
                ...corpo,
                dados: corpo.dados.map(({ id, identificador, nome, status, data_comunicacao, modelo, mqtt }) =>
                    ({ id, identificador, nome, status, data_comunicacao, modelo, mqtt }))
            } : corpo);
        }
        return next();
    });
});
app.use(api);
app.listen(13000, '127.0.0.1', () => console.log('Watt Vision TCC em http://127.0.0.1:13000/'));
