# Demo seed: wipe + insert. Run after Liquibase has created tables.
FROM node:22-alpine
WORKDIR /seed
COPY package.json yarn.lock .yarnrc.yml ./
RUN corepack enable && yarn install --immutable
COPY seed.mjs ./
CMD ["node", "seed.mjs"]
