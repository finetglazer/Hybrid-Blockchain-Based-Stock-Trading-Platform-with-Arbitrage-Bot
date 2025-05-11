# FE/investwebsite/Dockerfile
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application
COPY .. .

# Copy the Docker-specific Vite config
#COPY vite.config.js vite.config.txt

# Expose the port Vite runs on
EXPOSE 5173

# Run the dev server
CMD ["npm", "run", "dev"]