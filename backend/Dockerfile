# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 — vendor: build production Composer dependencies.
# ---------------------------------------------------------------------------
FROM composer:2 AS vendor

WORKDIR /app

COPY composer.json composer.lock ./

# --no-scripts: package scripts (e.g. auto-discovery) run in the runtime stage
# where the full application source is present.
RUN composer install \
    --no-dev \
    --no-interaction \
    --prefer-dist \
    --no-scripts \
    --no-progress \
    --optimize-autoloader

# ---------------------------------------------------------------------------
# Stage 2 — runtime: PHP-FPM with the ATU Cafeteria API.
# ---------------------------------------------------------------------------
FROM php:8.2-fpm-alpine AS runtime

WORKDIR /var/www/html

# PHP extensions required by the application and the PostgreSQL production database.
RUN set -eux; \
    apk add --no-cache --virtual .build-deps $PHPIZE_DEPS; \
    docker-php-ext-install pdo_pgsql pgsql bcmath; \
    apk del .build-deps

# Production-tuned PHP settings (kept revalidation ON so bind mounts and
# volume refreshes never serve stale bytecode).
RUN { \
        echo 'opcache.enable=1'; \
        echo 'opcache.memory_consumption=128'; \
        echo 'opcache.interned_strings_buffer=16'; \
        echo 'opcache.max_accelerated_files=10000'; \
        echo 'opcache.revalidate_freq=60'; \
        echo 'expose_php=0'; \
    } > /usr/local/etc/php/conf.d/zz-atu.ini

# Vendor from the build stage, then the application source.
COPY --from=vendor /app/vendor ./vendor
COPY . .

# Laravel writable paths.
RUN mkdir -p \
        storage/framework/cache/data \
        storage/framework/sessions \
        storage/framework/views \
        storage/logs \
        bootstrap/cache \
    && chown -R www-data:www-data storage bootstrap/cache

COPY docker/entrypoint.sh /usr/local/bin/docker-entrypoint
RUN chmod +x /usr/local/bin/docker-entrypoint

EXPOSE 9000

ENTRYPOINT ["docker-entrypoint"]
CMD ["php-fpm"]