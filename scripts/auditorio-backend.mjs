import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const backend = pathToFileURL(resolve('src') + '/');
let db;

try {
    const conexao = await import(new URL('config/database.js', backend));
    db = conexao.default;

    const [linhas] = await db.execute(`
        SELECT u.id
        FROM tbl_usuario u
        JOIN tbl_usuario_local ul ON ul.id_usuario = u.id
        JOIN tbl_local l ON l.id = ul.id_local AND l.id_empresa = u.id_empresa
        JOIN tbl_empresa e ON e.id = l.id_empresa
        WHERE e.cnpj = ? AND l.codigo = ? AND u.email = ? AND u.status = 'Ativo'
        LIMIT 1
    `, ['00.000.000/0000-00', 'TCC-LOC-01', 'operador.tcc@example.invalid']);

    if (linhas.length !== 1) {
        throw new Error('Base ou operador de apresentacao ausente. Siga o GUIA-TCC.md.');
    }

    const { default: app } = await import(new URL('app.js', backend));
    const servidor = app.listen(3000, '127.0.0.1', () => {
        console.log('Backend: http://127.0.0.1:3000/ | Web: http://127.0.0.1:5173/');
    });
    servidor.once('error', async (erro) => {
        console.error('Falha na porta 3000:', erro.code ?? erro.name);
        process.exitCode = 1;
        await db.end();
    });
} catch (erro) {
    console.error('Falha ao preparar o Backend:', erro.code ?? erro.message);
    process.exitCode = 1;
    if (db) await db.end();
}
