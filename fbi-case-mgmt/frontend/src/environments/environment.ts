export const environment = {
  production: false,
  apiBase: '/api/v1',
  auth: {
    issuer: 'http://localhost:8080/realms/Demo Only',
    clientId: 'Demo Only-web',
    responseType: 'code',
    scope: 'openid profile email',
    requireHttps: false
  }
};
