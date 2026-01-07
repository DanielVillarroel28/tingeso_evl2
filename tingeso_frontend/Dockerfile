# Ya no necesitamos la etapa de "builder" de Node porque Jenkins hará eso.
# Pasamos directo a la imagen que sirve el contenido.

FROM nginx:stable-alpine

# Copiamos la carpeta 'dist' que Jenkins generó previamente en el paso anterior
# IMPORTANTE: Asegúrate de que la ruta local sea 'dist' (o la que genere tu proyecto vite)
COPY dist /usr/share/nginx/html

# Copiamos tu configuración de nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]