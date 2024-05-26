FROM node:17-alpine

WORKDIR /app

COPY package.json ./
COPY package-lock.json ./

RUN npm install

COPY . .

ENV DOCKER_ENV=true

CMD ["npm", "start"]