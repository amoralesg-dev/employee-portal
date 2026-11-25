-- =====================================================================
-- SEED DE EJEMPLO (EXCLUSIVO PARA DEV - NO EJECUTAR EN PROD)
-- Esquema: iam
-- =====================================================================

USE iam;

-- 1. Noticia Hero de ejemplo
INSERT INTO portal_content (content_type, category, status, priority, publish_from, link_url, link_new_tab, created_at)
VALUES ('HERO_NEWS', 'CULTURA', 'PUBLISHED', 10, NOW(), NULL, 0, NOW());
SET @hero_id := LAST_INSERT_ID();

INSERT INTO portal_content_translation (portal_content_id, language, title, summary, body, link_label) VALUES
(@hero_id, 'es-MX', 'Bienvenidos al nuevo Portal Corporativo Rassini', 'Ahora Comunicación Interna puede publicar noticias, campañas y eventos en tiempo real.', 'Este espacio concentra las noticias más relevantes, campañas activas y eventos de todas nuestras plantas.\n\nConsulta diariamente las novedades de Seguridad, Personas, Negocio y Cultura.', 'Leer más'),
(@hero_id, 'en-US', 'Welcome to the new Rassini Corporate Portal', 'Internal Communications can now publish news, campaigns, and events in real time.', 'This space brings together key news, active campaigns, and events across all our plants.\n\nCheck back daily for updates on Safety, People, Business, and Culture.', 'Read more');

-- 2. Noticia secundaria de ejemplo
INSERT INTO portal_content (content_type, category, status, priority, publish_from, created_at)
VALUES ('NEWS', 'SEGURIDAD', 'PUBLISHED', 20, NOW(), NOW());
SET @news_id := LAST_INSERT_ID();

INSERT INTO portal_content_translation (portal_content_id, language, title, summary, body, link_label) VALUES
(@news_id, 'es-MX', 'Semana de Seguridad Industrial', 'Participa en los recorridos preventivos y talleres en tu planta.', 'Durante esta semana reforzaremos nuestros protocolos de cero accidentes en todas las áreas operativas.', 'Ver detalle'),
(@news_id, 'en-US', 'Industrial Safety Week', 'Join preventive walkthroughs and workshops at your plant.', 'This week we reinforce our zero-accident protocols across all operational areas.', 'View details');

-- 3. Acceso rápido de ejemplo
INSERT INTO portal_content (content_type, status, priority, publish_from, link_url, link_new_tab, icon, created_at)
VALUES ('QUICK_ACCESS', 'PUBLISHED', 10, NOW(), '/dashboard', 0, 'pi pi-home', NOW());
SET @qa_id := LAST_INSERT_ID();

INSERT INTO portal_content_translation (portal_content_id, language, title, summary, link_label) VALUES
(@qa_id, 'es-MX', 'Inicio del Portal', 'Regresa a la portada corporativa.', 'Ir'),
(@qa_id, 'en-US', 'Portal Home', 'Return to the corporate homepage.', 'Go');

-- 4. Evento de ejemplo
INSERT INTO portal_content (content_type, category, status, priority, publish_from, event_start, event_location, created_at)
VALUES ('EVENT', 'PERSONAS', 'PUBLISHED', 10, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'Auditorio Principal / Teams', NOW());
SET @ev_id := LAST_INSERT_ID();

INSERT INTO portal_content_translation (portal_content_id, language, title, summary) VALUES
(@ev_id, 'es-MX', 'Townhall Trimestral', 'Revisión de resultados y próximos proyectos.'),
(@ev_id, 'en-US', 'Quarterly Townhall', 'Results review and upcoming projects.');
