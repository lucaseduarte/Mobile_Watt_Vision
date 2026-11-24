// Atualiza o asset Android a partir do gerador e das dependências instaladas no Front_WV.
import { createRequire } from 'node:module';
import { readFile, readdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const front = resolve(process.argv[2] ?? '../Front_WV');
const assets = new URL('../app/src/main/assets/relatorios/', import.meta.url);
const requireFront = createRequire(resolve(front, 'package.json'));
const { rolldown } = await import(pathToFileURL(requireFront.resolve('rolldown')).href);
process.chdir(front);
import { gzipSync } from 'node:zlib';
const entrada = 'watt-vision-mobile-pdf';
const bundle = await rolldown({
  input: entrada,
  plugins: [{
    name: 'watt-vision-pdf-bridge',
    resolveId(id) {
      if (id === entrada) return id;
      if (['html2canvas', 'dompurify', 'canvg'].includes(id)) return 'wv-unused:' + id;
    },
    async load(id) {
      if (id.startsWith('wv-unused:')) {
        return 'export const Canvg = undefined; export default undefined;';
      }
      if (id !== entrada) return;
      let codigo = (await readFile('src/services/relatorioPDF.js', 'utf8')).replaceAll('\r\n', '\n');
      codigo = codigo.replace('  custoKwh = 0.8,', '  custoKwh = 0.8,\n  demonstracao = false,');
      codigo = codigo.replace("Setor: ${filtros.setor}`, PAGINA.margem + 5, 25);", "Setor: ${filtros.setor}${filtros.equipamento ? '   |   Equipamento: ' + filtros.equipamento : ''}`, PAGINA.margem + 5, 25);");
      const salvar = codigo.lastIndexOf('  doc.save(');
      if (salvar < 0) throw new Error('Não foi localizada a saída do gerador.');
      codigo = codigo.slice(0, salvar) + `
  if (demonstracao) {
    for (let pagina = 1; pagina <= doc.getNumberOfPages(); pagina++) {
      doc.setPage(pagina);
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(8);
      doc.setTextColor(180, 83, 9);
      doc.text('DEBUG · DADOS SIMULADOS', 148.5, 204, { align: 'center' });
    }
  }
  return { paginas: doc.getNumberOfPages(), pdf: doc.output('datauristring').split(',')[1] };
}
globalThis.gerarRelatorioWattVision = ({ registros, metricas, grandezas, filtros, custoKwh, demonstracao }) =>
  exportarRelatorioPDF({
    registros, metricas, metricasExibidas: metricas.filter(m => grandezas.includes(m.nome)),
    filtros, custoKwh, demonstracao,
    obterValor: (registro, metrica) => registro.valores[metrica.nome] ?? null,
    converterData: (dado) => new Date(dado)
  });
`;
      codigo = codigo.replace('  let anterior = null;\n  let quedas', '  const anteriores = new Map();\n  let quedas');
      codigo = codigo.replace('return { data, v, delta: 0 };', 'return { data, v, delta: 0, medidor: registro.medidor };');
      codigo = codigo.replace('    if (anterior != null && e >= anterior)', '    const anterior = anteriores.get(p.medidor);\n    if (anterior != null && e >= anterior)');
      codigo = codigo.replace('    anterior = e;', '    anteriores.set(p.medidor, e);');
      return codigo;
    }
  }]
});
try {
  const { output } = await bundle.generate({ format: 'iife', inlineDynamicImports: true, minify: true, sourcemap: false });
  const chunk = output.find(x => x.type === 'chunk');
  const modulos = Object.keys(chunk.modules)
    .map(caminho => caminho.replaceAll('\\', '/').split('/node_modules/').at(-1))
    .filter((_, indice) => Object.keys(chunk.modules)[indice].includes('node_modules'))
    .map(caminho => caminho.split('/').slice(0, caminho.startsWith('@') ? 2 : 1).join('/'));
  const licencas = [];
  for (const modulo of new Set(modulos)) {
    const pasta = resolve(front, 'node_modules', modulo);
    const arquivo = (await readdir(pasta)).find(nome => /^LICENSE(?:\.(txt|md))?$/i.test(nome));
    if (!arquivo) throw new Error('Licença indisponível: ' + modulo);
    licencas.push(modulo + '\n\n' + (await readFile(resolve(pasta, arquivo), 'utf8')).trim());
  }
  const base64 = gzipSync(chunk.code).toString('base64');
  await writeFile(new URL('desktop-pdf.js.gz.base64', assets), base64.match(/.{1,2600}/g).join('\n') + '\n');
  await writeFile(new URL('desktop-pdf-licencas.txt', assets), licencas.join('\n\n' + '-'.repeat(72) + '\n\n') + '\n');
} finally { await bundle.close(); }
