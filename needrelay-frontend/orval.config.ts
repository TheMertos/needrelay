import { defineConfig } from 'orval';

/**
 * Orval config: generates a typed Axios client from the OpenAPI contract.
 */
export default defineConfig({
  needrelay: {
    input: './openapi/openapi.yaml',
    output: {
      mode: 'tags-split',
      target: './src/api/generated',
      schemas: './src/api/generated/models',
      client: 'axios',
      override: {
        mutator: {
          path: './src/api/client.ts',
          name: 'customInstance',
        },
      },
    },
  },
});
