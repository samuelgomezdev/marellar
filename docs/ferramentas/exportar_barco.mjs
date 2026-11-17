// Exporta o modelo 3D de uma linha para o jogo (Godot): client-godot/assets/barcos/marellar-<linha>.glb e .json.
//
// A geometria continua nascendo na pagina three.js (marellar-api/.../static/inspecao/index.html); este script
// so abre a pagina em ?exportar=<linha> num Chromium sem janela e grava o que ela gerou.
//
// Uso (precisa do Playwright, que NAO e dependencia do projeto; use um ja instalado na maquina):
//   node docs/ferramentas/exportar_barco.mjs 33
//   MARELLAR_PLAYWRIGHT=/caminho/node_modules/playwright-core node docs/ferramentas/exportar_barco.mjs 33
import { createServer } from 'node:http';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { extname, join, normalize, dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const pagina = join(raiz, 'marellar-api', 'src', 'main', 'resources', 'static');
const saida = join(raiz, 'client-godot', 'assets', 'barcos');
const linhas = process.argv.slice(2).length ? process.argv.slice(2) : ['33'];

const caminhoPw = process.env.MARELLAR_PLAYWRIGHT;
const { chromium } = caminhoPw
  ? await import(pathToFileURL(join(caminhoPw, 'index.mjs')).href).catch(() => import(pathToFileURL(join(caminhoPw, 'index.js')).href))
  : await import('playwright');

const tipos = { '.html': 'text/html', '.js': 'text/javascript', '.json': 'application/json', '.txt': 'text/plain' };
const servidor = createServer(async (req, res) => {
  const url = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  const arquivo = normalize(join(pagina, url.endsWith('/') ? url + 'index.html' : url));
  if (!arquivo.startsWith(pagina)) { res.writeHead(403).end(); return; }
  try {
    const corpo = await readFile(arquivo);
    res.writeHead(200, { 'Content-Type': tipos[extname(arquivo)] || 'application/octet-stream' }).end(corpo);
  } catch { res.writeHead(404).end(); }   // /api/... da 404: a pagina entra no modo sem servidor
});
await new Promise(ok => servidor.listen(0, '127.0.0.1', ok));
const porta = servidor.address().port;

const navegador = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
try {
  await mkdir(saida, { recursive: true });
  for (const linha of linhas) {
    const page = await navegador.newPage();
    page.on('pageerror', e => console.error('erro na pagina:', e.message));
    await page.goto(`http://127.0.0.1:${porta}/inspecao/index.html?exportar=${linha}`);
    await page.waitForFunction(() => window.__exportado, null, { timeout: 120000 });
    const r = await page.evaluate(() => window.__exportado.erro
      ? { erro: window.__exportado.erro }
      : { glb: Array.from(window.__exportado.glb), json: window.__exportado.json });
    if (r.erro) throw new Error(`linha ${linha}: ${r.erro}`);
    const glb = Buffer.from(r.glb);
    // o formulario de NC do jogo usa o catalogo ampliado de data/ (quem valida e o motor)
    const info0 = JSON.parse(r.json);
    const catalogo = JSON.parse(await readFile(join(raiz, 'data', 'nc-catalogo-inspecao.json'), 'utf-8'));
    info0.categorias = catalogo.categorias;
    info0.defeitos = catalogo.tipos;
    r.json = JSON.stringify(info0, null, 1);
    await writeFile(join(saida, `marellar-${linha}.glb`), glb);
    await writeFile(join(saida, `marellar-${linha}.json`), r.json + '\n');
    const info = JSON.parse(r.json);
    console.log(`linha ${linha}: ${(glb.length / 1048576).toFixed(2)} MB, ${info.itens.length} itens, ${info.compartimentos.length} compartimentos`);
    await page.close();
  }
} finally {
  await navegador.close();
  servidor.close();
}
