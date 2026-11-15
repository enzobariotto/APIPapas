'use strict';

/*
 * Front-end simples da API de Papas.
 * É servido pelo próprio Spring Boot (src/main/resources/static), então chama a API na mesma origem.
 */

const TAMANHO_PAGINA = 10;
const SITUACOES = ['EM_EXERCICIO', 'FALECIMENTO', 'RENUNCIA'];

const fmtData = v => (v ? v.split('-').reverse().join('/') : '—');
const fmtSituacao = v => ({ EM_EXERCICIO: 'Em exercício', FALECIMENTO: 'Falecimento', RENUNCIA: 'Renúncia' })[v] ?? v;
const nomePapa = p => (p ? p.nomePapal : '—');

/*
 * Configuração de cada entidade: colunas da tabela, campos do formulário e a consulta personalizada.
 * Tipos de campo: text, number, date, enum (lista fixa), papa (um papa), papas (vários papas).
 */
const ENTIDADES = {
    papas: {
        titulo: 'Papas', singular: 'papa', ordem: 'numeroOrdem',
        colunas: [
            ['Nº', p => p.numeroOrdem],
            ['Nome papal', p => p.nomePapal],
            ['Nome de batismo', p => p.nomeBatismo],
            ['País', p => p.paisNascimento],
            ['Início', p => fmtData(p.inicioPontificado)],
            ['Fim', p => fmtData(p.fimPontificado)],
            ['Situação', p => fmtSituacao(p.situacao)],
        ],
        campos: [
            { nome: 'nomePapal', rotulo: 'Nome papal', tipo: 'text' },
            { nome: 'nomeBatismo', rotulo: 'Nome de batismo', tipo: 'text' },
            { nome: 'numeroOrdem', rotulo: 'Número de ordem', tipo: 'number' },
            { nome: 'paisNascimento', rotulo: 'País de nascimento', tipo: 'text' },
            { nome: 'inicioPontificado', rotulo: 'Início do pontificado', tipo: 'date' },
            { nome: 'fimPontificado', rotulo: 'Fim do pontificado', tipo: 'date', opcional: true },
            { nome: 'situacao', rotulo: 'Situação', tipo: 'enum', opcoes: SITUACOES, formatar: fmtSituacao },
        ],
        busca: {
            rotulo: 'Situação', tipo: 'enum', opcoes: SITUACOES, formatar: fmtSituacao,
            url: v => `/papas/situacao/${encodeURIComponent(v)}`,
        },
    },
    conclaves: {
        titulo: 'Conclaves', singular: 'conclave', ordem: 'dataInicio',
        colunas: [
            ['Papa eleito', c => nomePapa(c.papa)],
            ['Início', c => fmtData(c.dataInicio)],
            ['Término', c => fmtData(c.dataFim)],
            ['Cardeais', c => c.numeroCardeais],
            ['Escrutínios', c => c.numeroEscrutinios],
        ],
        campos: [
            { nome: 'papa', rotulo: 'Papa eleito', tipo: 'papa' },
            { nome: 'dataInicio', rotulo: 'Data de início', tipo: 'date' },
            { nome: 'dataFim', rotulo: 'Data de término', tipo: 'date' },
            { nome: 'numeroCardeais', rotulo: 'Número de cardeais', tipo: 'number' },
            { nome: 'numeroEscrutinios', rotulo: 'Número de escrutínios', tipo: 'number' },
        ],
        busca: {
            rotulo: 'Máximo de escrutínios', tipo: 'number',
            url: v => `/conclaves/rapidos?maxEscrutinios=${encodeURIComponent(v)}`,
        },
    },
    enciclicas: {
        titulo: 'Encíclicas', singular: 'encíclica', ordem: 'dataPublicacao',
        colunas: [
            ['Título', e => e.titulo],
            ['Tradução', e => e.tituloPortugues || '—'],
            ['Publicação', e => fmtData(e.dataPublicacao)],
            ['Papa', e => nomePapa(e.papa)],
        ],
        campos: [
            { nome: 'titulo', rotulo: 'Título (latim)', tipo: 'text' },
            { nome: 'tituloPortugues', rotulo: 'Título em português', tipo: 'text', opcional: true },
            { nome: 'dataPublicacao', rotulo: 'Data de publicação', tipo: 'date' },
            { nome: 'papa', rotulo: 'Papa', tipo: 'papa' },
        ],
        busca: {
            rotulo: 'Papa', tipo: 'papa',
            url: v => `/enciclicas/papa/${encodeURIComponent(v)}`,
        },
    },
    concilios: {
        titulo: 'Concílios', singular: 'concílio', ordem: 'dataInicio',
        colunas: [
            ['Nome', c => c.nome],
            ['Local', c => c.local],
            ['Início', c => fmtData(c.dataInicio)],
            ['Encerramento', c => fmtData(c.dataFim)],
            ['Papas', c => (c.papas ?? []).map(nomePapa).join(', ') || '—'],
        ],
        campos: [
            { nome: 'nome', rotulo: 'Nome', tipo: 'text' },
            { nome: 'local', rotulo: 'Local', tipo: 'text' },
            { nome: 'dataInicio', rotulo: 'Data de início', tipo: 'date' },
            { nome: 'dataFim', rotulo: 'Data de encerramento', tipo: 'date' },
            { nome: 'papas', rotulo: 'Papas', tipo: 'papas' },
        ],
        busca: {
            rotulo: 'Nome contém', tipo: 'text',
            url: v => `/concilios/busca?nome=${encodeURIComponent(v)}`,
        },
    },
    santos: {
        titulo: 'Santos', singular: 'santo', ordem: 'dataCanonizacao',
        colunas: [
            ['Nome', s => s.nome],
            ['País de origem', s => s.paisOrigem],
            ['Canonização', s => fmtData(s.dataCanonizacao)],
            ['Canonizado por', s => nomePapa(s.canonizadoPor)],
        ],
        campos: [
            { nome: 'nome', rotulo: 'Nome', tipo: 'text' },
            { nome: 'paisOrigem', rotulo: 'País de origem', tipo: 'text' },
            { nome: 'dataCanonizacao', rotulo: 'Data de canonização', tipo: 'date' },
            { nome: 'canonizadoPor', rotulo: 'Canonizado por', tipo: 'papa' },
        ],
        busca: {
            rotulo: 'País de origem', tipo: 'text',
            url: v => `/santos/pais/${encodeURIComponent(v)}`,
        },
    },
};

const estado = {
    entidade: 'papas',
    pagina: 0,
    busca: null,          // valor da consulta personalizada ativa (null = listagem normal)
    papas: [],            // usado nos campos de seleção de papa
    editando: null,       // item em edição (null = cadastro)
    chaveIdempotencia: null,
    requisicao: 0,        // evita que uma resposta antiga sobrescreva uma mais nova
};

const $ = seletor => document.querySelector(seletor);

// ------------------------------------------------------------------ chamadas à API

class ErroDaApi extends Error {
    constructor(status, dados, retryAfter) {
        super(dados?.mensagem ?? `Erro ${status}`);
        this.status = status;
        this.dados = dados;
        this.retryAfter = retryAfter;
    }
}

function lerChaveApi() {
    try {
        return localStorage.getItem('chaveApi') ?? '';
    } catch {
        return '';
    }
}

function salvarChaveApi(valor) {
    try {
        localStorage.setItem('chaveApi', valor);
    } catch {
        // sem armazenamento (aba anônima etc.): a chave vale só enquanto a página estiver aberta
    }
}

function novaChaveIdempotencia() {
    if (crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
}

async function api(metodo, url, corpo, chaveIdempotencia) {
    const headers = {};
    const chave = $('#chave-api').value.trim();
    if (chave) {
        headers['X-API-Key'] = chave;
    }
    if (corpo !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    if (chaveIdempotencia) {
        headers['X-Idempotency-Key'] = chaveIdempotencia;
    }

    let resposta;
    try {
        resposta = await fetch(url, {
            method: metodo,
            headers,
            body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
        });
    } catch {
        throw new ErroDaApi(0, { mensagem: 'Não foi possível conectar à API. Ela está rodando?' });
    }

    if (resposta.status === 204) {
        return null;
    }
    const dados = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        throw new ErroDaApi(resposta.status, dados, resposta.headers.get('Retry-After'));
    }
    return dados;
}

/** Monta um texto legível a partir do formato ErroApi devolvido pela API. */
function textoDoErro(erro, incluirCampos = true) {
    let texto = erro.status ? `Erro ${erro.status}: ${erro.message}` : erro.message;
    if (erro.status === 401) {
        texto += '\nInforme uma chave de API válida no topo da página.';
    }
    if (erro.status === 429 && erro.retryAfter) {
        texto += `\nTente novamente em ${erro.retryAfter} segundos.`;
    }
    const campos = erro.dados?.campos;
    if (incluirCampos && campos) {
        texto += '\n' + Object.entries(campos).map(([campo, msg]) => `• ${campo}: ${msg}`).join('\n');
    }
    return texto;
}

// ------------------------------------------------------------------ mensagens

let temporizadorMensagem;

function mostrarMensagem(texto, tipo) {
    const caixa = $('#mensagem');
    caixa.textContent = texto;
    caixa.className = `mensagem ${tipo}`;
    caixa.hidden = false;
    clearTimeout(temporizadorMensagem);
    if (tipo === 'ok') {
        temporizadorMensagem = setTimeout(() => (caixa.hidden = true), 4000);
    }
}

function esconderMensagem() {
    $('#mensagem').hidden = true;
}

// ------------------------------------------------------------------ abas e busca

function renderAbas() {
    const nav = $('#abas');
    nav.replaceChildren();
    for (const [chave, cfg] of Object.entries(ENTIDADES)) {
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.setAttribute('role', 'tab');
        botao.textContent = cfg.titulo;
        botao.setAttribute('aria-selected', String(chave === estado.entidade));
        botao.addEventListener('click', () => trocarEntidade(chave));
        nav.append(botao);
    }
}

function trocarEntidade(chave) {
    estado.entidade = chave;
    estado.pagina = 0;
    estado.busca = null;
    esconderMensagem();
    renderAbas();
    renderBusca();
    carregar();
}

function criarControleDeBusca(cfg) {
    if (cfg.tipo === 'enum' || cfg.tipo === 'papa') {
        const select = document.createElement('select');
        select.append(new Option('Selecione…', ''));
        const opcoes = cfg.tipo === 'enum'
            ? cfg.opcoes.map(o => [o, cfg.formatar ? cfg.formatar(o) : o])
            : estado.papas.map(p => [p.id, p.nomePapal]);
        for (const [valor, texto] of opcoes) {
            select.append(new Option(texto, valor));
        }
        return select;
    }
    const input = document.createElement('input');
    input.type = cfg.tipo === 'number' ? 'number' : 'text';
    if (cfg.tipo === 'number') {
        input.min = '1';
    }
    return input;
}

async function renderBusca() {
    const entidade = estado.entidade;
    const cfg = ENTIDADES[entidade].busca;
    if (cfg.tipo === 'papa') {
        await carregarPapas();
        if (entidade !== estado.entidade) {
            return; // o usuário trocou de aba enquanto os papas carregavam
        }
    }
    const form = $('#busca');
    form.replaceChildren();

    const controle = criarControleDeBusca(cfg);
    controle.id = 'valor-busca';
    if (estado.busca) {
        controle.value = estado.busca;
    }
    const rotulo = document.createElement('label');
    rotulo.htmlFor = 'valor-busca';
    rotulo.textContent = `Buscar por ${cfg.rotulo.toLowerCase()}:`;

    const buscar = document.createElement('button');
    buscar.type = 'submit';
    buscar.textContent = 'Buscar';

    const limpar = document.createElement('button');
    limpar.type = 'button';
    limpar.textContent = 'Limpar';
    limpar.addEventListener('click', () => {
        estado.busca = null;
        estado.pagina = 0;
        controle.value = '';
        esconderMensagem();
        carregar();
    });

    form.append(rotulo, controle, buscar, limpar);
}

$('#busca').addEventListener('submit', evento => {
    evento.preventDefault();
    const valor = $('#valor-busca').value.trim();
    estado.busca = valor || null;
    estado.pagina = 0;
    esconderMensagem();
    carregar();
});

// ------------------------------------------------------------------ listagem

async function carregarPapas() {
    try {
        const dados = await api('GET', '/papas?size=100&sort=numeroOrdem');
        estado.papas = Object.values(dados?._embedded ?? {})[0] ?? [];
    } catch (erro) {
        mostrarMensagem('Não foi possível carregar a lista de papas.\n' + textoDoErro(erro), 'erro');
    }
}

async function carregar() {
    const cfg = ENTIDADES[estado.entidade];
    const requisicao = ++estado.requisicao;
    const base = estado.busca ? cfg.busca.url(estado.busca) : `/${estado.entidade}`;
    const parametros = new URLSearchParams({ page: estado.pagina, size: TAMANHO_PAGINA, sort: cfg.ordem });
    const url = base + (base.includes('?') ? '&' : '?') + parametros;

    renderCabecalho(cfg);
    renderLinhaUnica(cfg, 'Carregando…');

    try {
        const dados = await api('GET', url);
        if (requisicao !== estado.requisicao) {
            return;
        }
        // Resposta HAL do PagedModel: os itens ficam em _embedded.<nome da lista>
        const itens = Object.values(dados?._embedded ?? {})[0] ?? [];
        renderLinhas(cfg, itens);
        renderPaginacao(dados?.page);
    } catch (erro) {
        if (requisicao !== estado.requisicao) {
            return;
        }
        mostrarMensagem(textoDoErro(erro), 'erro');
        renderLinhaUnica(cfg, 'Não foi possível carregar os registros.');
        renderPaginacao(null);
    }
}

function renderCabecalho(cfg) {
    const linha = document.createElement('tr');
    for (const [titulo] of cfg.colunas) {
        const th = document.createElement('th');
        th.textContent = titulo;
        linha.append(th);
    }
    linha.append(document.createElement('th'));
    $('thead').replaceChildren(linha);
}

function renderLinhaUnica(cfg, texto) {
    const td = document.createElement('td');
    td.colSpan = cfg.colunas.length + 1;
    td.className = 'vazio';
    td.textContent = texto;
    const linha = document.createElement('tr');
    linha.append(td);
    $('tbody').replaceChildren(linha);
}

function renderLinhas(cfg, itens) {
    if (itens.length === 0) {
        renderLinhaUnica(cfg, 'Nenhum registro encontrado.');
        return;
    }
    const linhas = itens.map(item => {
        const linha = document.createElement('tr');
        for (const [, valor] of cfg.colunas) {
            const td = document.createElement('td');
            td.textContent = valor(item) ?? '—';
            linha.append(td);
        }
        const acoes = document.createElement('td');
        acoes.className = 'acoes';

        const editar = document.createElement('button');
        editar.type = 'button';
        editar.textContent = 'Editar';
        editar.addEventListener('click', () => abrirFormulario(item));

        const excluir = document.createElement('button');
        excluir.type = 'button';
        excluir.className = 'perigo';
        excluir.textContent = 'Excluir';
        excluir.addEventListener('click', () => excluirItem(item, itens.length));

        acoes.append(editar, excluir);
        linha.append(acoes);
        return linha;
    });
    $('tbody').replaceChildren(...linhas);
}

function renderPaginacao(pagina) {
    const total = pagina?.totalPages ?? 0;
    const atual = pagina?.number ?? 0;
    $('#info-pagina').textContent = total > 0
        ? `Página ${atual + 1} de ${total} · ${pagina.totalElements} registro(s)`
        : '';
    $('#anterior').disabled = atual <= 0;
    $('#proxima').disabled = atual + 1 >= total;
}

$('#anterior').addEventListener('click', () => {
    estado.pagina--;
    carregar();
});

$('#proxima').addEventListener('click', () => {
    estado.pagina++;
    carregar();
});

// ------------------------------------------------------------------ links HATEOAS

/** Usa o link HATEOAS devolvido pela API (update/delete); se não houver, monta a URL. */
function linkDe(item, rel) {
    return item._links?.[rel]?.href ?? `/${estado.entidade}/${item.id}`;
}

// ------------------------------------------------------------------ exclusão

async function excluirItem(item, itensNaPagina) {
    const cfg = ENTIDADES[estado.entidade];
    const descricao = cfg.colunas[0][1](item);
    if (!confirm(`Excluir ${cfg.singular} "${descricao}"?`)) {
        return;
    }
    try {
        await api('DELETE', linkDe(item, 'delete'));
        mostrarMensagem(`Registro "${descricao}" excluído.`, 'ok');
        if (itensNaPagina === 1 && estado.pagina > 0) {
            estado.pagina--;
        }
        carregar();
    } catch (erro) {
        mostrarMensagem(textoDoErro(erro), 'erro');
    }
}

// ------------------------------------------------------------------ formulário

function criarCampo(campo, item) {
    const valorAtual = item?.[campo.nome];

    if (campo.tipo === 'papas') {
        const fieldset = document.createElement('fieldset');
        fieldset.className = 'campo';
        fieldset.dataset.campo = campo.nome;
        const legenda = document.createElement('legend');
        legenda.textContent = campo.rotulo;
        const lista = document.createElement('div');
        lista.className = 'lista-papas';
        const selecionados = new Set((valorAtual ?? []).map(p => p.id));
        for (const papa of estado.papas) {
            const rotulo = document.createElement('label');
            const caixa = document.createElement('input');
            caixa.type = 'checkbox';
            caixa.name = campo.nome;
            caixa.value = papa.id;
            caixa.checked = selecionados.has(papa.id);
            rotulo.append(caixa, papa.nomePapal);
            lista.append(rotulo);
        }
        fieldset.append(legenda, lista, criarErroDoCampo());
        return fieldset;
    }

    const div = document.createElement('div');
    div.className = 'campo';
    div.dataset.campo = campo.nome;
    const id = `campo-${campo.nome}`;

    const rotulo = document.createElement('label');
    rotulo.htmlFor = id;
    rotulo.textContent = campo.rotulo;
    if (campo.opcional) {
        const opcional = document.createElement('span');
        opcional.className = 'opcional';
        opcional.textContent = ' (opcional)';
        rotulo.append(opcional);
    }

    let controle;
    if (campo.tipo === 'enum' || campo.tipo === 'papa') {
        controle = document.createElement('select');
        controle.append(new Option('Selecione…', ''));
        const opcoes = campo.tipo === 'enum'
            ? campo.opcoes.map(o => [o, campo.formatar ? campo.formatar(o) : o])
            : estado.papas.map(p => [p.id, `${p.nomePapal} (nº ${p.numeroOrdem})`]);
        for (const [valor, texto] of opcoes) {
            controle.append(new Option(texto, valor));
        }
        controle.value = campo.tipo === 'papa' ? (valorAtual?.id ?? '') : (valorAtual ?? '');
    } else {
        controle = document.createElement('input');
        controle.type = campo.tipo;
        controle.value = valorAtual ?? '';
    }
    controle.id = id;
    controle.name = campo.nome;

    div.append(rotulo, controle, criarErroDoCampo());
    return div;
}

function criarErroDoCampo() {
    const erro = document.createElement('span');
    erro.className = 'erro-campo';
    erro.hidden = true;
    return erro;
}

function lerCampo(campo, form) {
    if (campo.tipo === 'papas') {
        return [...form.querySelectorAll(`input[name="${campo.nome}"]:checked`)]
            .map(caixa => ({ id: Number(caixa.value) }));
    }
    const valor = form.elements[campo.nome].value.trim();
    if (valor === '') {
        return null;
    }
    if (campo.tipo === 'number') {
        return Number(valor);
    }
    if (campo.tipo === 'papa') {
        return { id: Number(valor) };
    }
    return valor;
}

async function abrirFormulario(item) {
    const cfg = ENTIDADES[estado.entidade];
    estado.editando = item ?? null;
    // Uma chave de idempotência por cadastro: se o usuário clicar em Salvar de novo
    // (ex.: após uma falha de rede), a API não cadastra o registro duas vezes.
    estado.chaveIdempotencia = item ? null : novaChaveIdempotencia();

    if (cfg.campos.some(c => c.tipo === 'papa' || c.tipo === 'papas')) {
        await carregarPapas();
    }

    $('#titulo-form').textContent = item ? `Editar ${cfg.singular}` : `Cadastrar ${cfg.singular}`;
    $('#erro-form').hidden = true;
    $('#campos').replaceChildren(...cfg.campos.map(c => criarCampo(c, item)));
    $('#dialogo').showModal();
    $('#campos').querySelector('input, select')?.focus();
}

function limparErrosDoFormulario() {
    $('#erro-form').hidden = true;
    for (const erro of document.querySelectorAll('#campos .erro-campo')) {
        erro.hidden = true;
    }
}

function mostrarErrosNoFormulario(erro) {
    const cfg = ENTIDADES[estado.entidade];
    const semCampo = {};
    for (const [chave, mensagem] of Object.entries(erro.dados?.campos ?? {})) {
        // "papa.id" e "papas[0]" pertencem aos campos "papa" e "papas"
        const campo = cfg.campos.find(c =>
            chave === c.nome || chave.startsWith(c.nome + '.') || chave.startsWith(c.nome + '['));
        const destino = campo && document.querySelector(`#campos [data-campo="${campo.nome}"] .erro-campo`);
        if (destino) {
            destino.textContent = destino.hidden ? mensagem : `${destino.textContent}; ${mensagem}`;
            destino.hidden = false;
        } else {
            semCampo[chave] = mensagem;
        }
    }
    let texto = textoDoErro(erro, false);
    const extras = Object.values(semCampo);
    if (extras.length) {
        texto += '\n' + extras.map(m => `• ${m}`).join('\n');
    }
    $('#erro-form').textContent = texto;
    $('#erro-form').hidden = false;
}

$('#formulario').addEventListener('submit', async evento => {
    evento.preventDefault();
    const cfg = ENTIDADES[estado.entidade];
    const form = evento.target;
    limparErrosDoFormulario();

    const corpo = {};
    for (const campo of cfg.campos) {
        corpo[campo.nome] = lerCampo(campo, form);
    }

    const salvar = $('#salvar');
    salvar.disabled = true;
    try {
        if (estado.editando) {
            await api('PUT', linkDe(estado.editando, 'update'), corpo);
        } else {
            await api('POST', `/${estado.entidade}`, corpo, estado.chaveIdempotencia);
        }
        $('#dialogo').close();
        mostrarMensagem(estado.editando ? 'Registro atualizado.' : 'Registro cadastrado.', 'ok');
        carregar();
    } catch (erro) {
        mostrarErrosNoFormulario(erro);
    } finally {
        salvar.disabled = false;
    }
});

$('#cancelar').addEventListener('click', () => $('#dialogo').close());
$('#novo').addEventListener('click', () => abrirFormulario(null));

// ------------------------------------------------------------------ início

$('#chave-api').value = lerChaveApi();
$('#chave-api').addEventListener('change', evento => salvarChaveApi(evento.target.value.trim()));

renderAbas();
renderBusca();
carregar();
