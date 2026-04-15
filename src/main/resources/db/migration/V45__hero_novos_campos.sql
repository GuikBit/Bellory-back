-- Novos campos de configuração do hero
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_content_layout VARCHAR(30) DEFAULT 'text-left';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_title_size VARCHAR(10) DEFAULT 'lg';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_height VARCHAR(10) DEFAULT '85vh';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_overlay_style VARCHAR(30) DEFAULT 'gradient-bottom';
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_badge_text VARCHAR(255);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_title_highlight VARCHAR(255);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_show_particles BOOLEAN DEFAULT true;
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_video_url VARCHAR(500);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_side_image_url VARCHAR(500);
ALTER TABLE app.site_publico_config ADD COLUMN IF NOT EXISTS hero_stats_config TEXT;
