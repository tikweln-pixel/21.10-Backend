-- V002__Add_weighted_score_columns_to_votings.sql
-- Añade las columnas weighted_score y weighting_strategy a la tabla votings
-- para soportar el sistema de votación ponderada

ALTER TABLE votings
ADD COLUMN IF NOT EXISTS weighted_score DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS weighting_strategy VARCHAR(255);

-- Comentarios descriptivos
COMMENT ON COLUMN votings.weighted_score IS 'Puntuación ponderada del voto aplicando la estrategia seleccionada de la categoría';
COMMENT ON COLUMN votings.weighting_strategy IS 'Clave de la estrategia de ponderación aplicada (ej: categoryFactor, default)';
