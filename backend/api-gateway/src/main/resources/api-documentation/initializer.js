"use strict";
window.ui = SwaggerUIBundle({
  url: "/docs/openapi/backend.openapi.json",
  dom_id: "#swagger-ui",
  deepLinking: true,
  displayRequestDuration: true,
  filter: true,
  validatorUrl: null,
  persistAuthorization: false,
  withCredentials: false,
  queryConfigEnabled: false,
  tryItOutEnabled: false,
  supportedSubmitMethods: ["get", "post", "put", "patch"],
  requestInterceptor: function (request) {
    if (new URL(request.url, window.location.origin).origin !== window.location.origin) {
      throw new Error("Las pruebas de esta documentación utilizan el mismo gateway.");
    }
    return request;
  }
});
