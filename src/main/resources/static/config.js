'use strict';

/*
 * Endereço da API usado pelo front-end.
 *
 * - Servido pelo próprio Spring Boot (localhost ou Render): mesma origem, então fica vazio.
 * - Servido em outro lugar (ex.: Vercel): chama a API publicada no Render. Nesse caso o
 *   endereço do front precisa estar na variável CORS_ORIGENS do Render.
 */
const API_PUBLICADA = 'https://apipapas.onrender.com';

const servidoPelaApi = ['localhost', '127.0.0.1'].includes(location.hostname)
    || location.hostname.endsWith('.onrender.com');

const API_URL = servidoPelaApi ? '' : API_PUBLICADA;
