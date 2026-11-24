import { createRequire } from 'node:module';
import { resolve } from 'node:path';

const [backend, porta = '3306'] = process.argv.slice(2);
const email = 'operador.tcc@example.invalid';
let banco;

try {
    if (!backend || !/^\d+$/.test(porta) || Number(porta) < 1 || Number(porta) > 65535 || process.stdin.isTTY) {
        throw new Error('Informe a pasta do Backend, a porta local e envie as credenciais por stdin.');
    }
    const carregar = createRequire(resolve(backend, 'package.json'));
    const mysql = carregar('mysql2/promise');
    const bcrypt = carregar('bcrypt');

    process.stdin.setEncoding('utf8');
    let entrada = '';
    for await (const parte of process.stdin) entrada += parte;
    const { admin, senhaAdmin, senhaOperador } = JSON.parse(entrada.replace(/^\uFEFF/, ''));
    entrada = '';

    if (typeof admin !== 'string' || !admin.trim() ||
        typeof senhaAdmin !== 'string' || !senhaAdmin ||
        typeof senhaOperador !== 'string' || Buffer.byteLength(senhaOperador, 'utf8') < 12 ||
        Buffer.byteLength(senhaOperador, 'utf8') > 72 || senhaOperador === senhaAdmin) {
        throw new Error('Informe senhas distintas e use de 12 a 72 bytes na senha do operador.');
    }

    banco = await mysql.createConnection({
        host: '127.0.0.1', port: Number(porta), user: admin, password: senhaAdmin,
        database: 'db_watt_vision', connectTimeout: 5000
    });

    await banco.beginTransaction();
    const [base] = await banco.execute(
        `SELECT l.id FROM tbl_empresa e
         JOIN tbl_local l ON l.id_empresa = e.id
         WHERE e.id = 1 AND e.cnpj = '00.000.000/0000-00'
           AND l.id = 1 AND l.codigo = 'TCC-LOC-01'
           AND NOT EXISTS (SELECT 1 FROM tbl_usuario)
         FOR UPDATE`
    );
    if (base.length !== 1) {
        throw new Error('A base não é a demonstração vazia preparada por tcc-demonstracao.sql.');
    }
    const hash = await bcrypt.hash(senhaOperador, 12);
    const [usuario] = await banco.execute(
        "INSERT INTO tbl_usuario (id_empresa, nome, email, senha, perfil) VALUES (1, ?, ?, ?, 'Gestor')",
        ['Operador TCC', email, hash]
    );
    await banco.execute(
        'INSERT INTO tbl_usuario_local (id_usuario, id_local) VALUES (?, ?)',
        [usuario.insertId, base[0].id]
    );
    await banco.commit();
    console.log(`Conta fictícia provisionada: ${email} (Gestor).`);
} catch (erro) {
    if (banco) await banco.rollback().catch(() => {});
    console.error(erro.code ? `Falha no provisionamento (${erro.code}).` :
        erro instanceof SyntaxError ? 'Entrada JSON inválida.' : erro.message);
    process.exitCode = 1;
} finally {
    if (banco) await banco.end();
}
