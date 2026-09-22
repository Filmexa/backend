-- Seed data for movie_category.
-- Runs on every startup, after Hibernate has created/updated the schema
-- (see spring.sql.init.mode and spring.jpa.defer-datasource-initialization).
-- Re-running is safe: rows are matched on genre_id and refreshed.

INSERT INTO movie_category
    (id, name_en, name_fr, name_ar, description, genre_id, active, display_order, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'Top Rated',       'Les mieux notés', 'الأعلى تقييماً', 'Top rated movies',       100,     true, 1,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Action',          'Action',          'أكشن',          'Action movies',          28,    true, 2,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Adventure',       'Aventure',        'مغامرة',        'Adventure movies',       12,    true, 3,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Animation',       'Animation',       'رسوم متحركة',   'Animation movies',       16,    true, 4,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Comedy',          'Comédie',         'كوميديا',       'Comedy movies',          35,    true, 5,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Crime',           'Crime',           'جريمة',         'Crime movies',           80,    true, 6,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Documentary',     'Documentaire',    'وثائقي',        'Documentary movies',     99,    true, 7,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Drama',           'Drame',           'دراما',         'Drama movies',           18,    true, 8,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Family',          'Familial',        'عائلي',         'Family movies',          10751, true, 9,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Fantasy',         'Fantastique',     'فانتازيا',      'Fantasy movies',         14,    true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'History',         'Histoire',        'تاريخي',        'History movies',         36,    true, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Horror',          'Horreur',         'رعب',           'Horror movies',          27,    true, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Music',           'Musique',         'موسيقى',        'Music movies',           10402, true, 13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Mystery',         'Mystère',         'غموض',          'Mystery movies',         9648,  true, 14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Romance',         'Romance',         'رومانسي',       'Romance movies',         10749, true, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Science Fiction', 'Science-fiction', 'خيال علمي',     'Science fiction movies', 878,   true, 16, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'TV Movie',        'Téléfilm',        'فيلم تلفزيوني', 'TV movies',              10770, true, 17, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Thriller',        'Thriller',        'إثارة',         'Thriller movies',        53,    true, 18, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'War',             'Guerre',          'حرب',           'War movies',             10752, true, 19, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'Western',         'Western',         'غربي',          'Western movies',         37,    true, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (genre_id) DO UPDATE SET
    name_en       = EXCLUDED.name_en,
    name_fr       = EXCLUDED.name_fr,
    name_ar       = EXCLUDED.name_ar,
    description   = EXCLUDED.description,
    display_order = EXCLUDED.display_order,
    updated_at    = CURRENT_TIMESTAMP;
