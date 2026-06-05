import { createOpenAPI } from "fumadocs-openapi/server";

export const openapi = createOpenAPI({
  input: [process.env.OPENAPI_INPUT ?? "http://localhost:8080/api/v3/api-docs"],
});
