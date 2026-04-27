package com.votify.entity;

/**
 * Tipo de votación asociada a una categoría, según el diagrama de clases de Votify.
 *
 * JURY_EXPERT  → Votacion_Jurado_Exp : votación realizada por jurado experto
 * POPULAR_VOTE → Voto_Popular        : votación abierta al público en general
 */
public enum VotingType {
    JURY_EXPERT,
    POPULAR_VOTE
}
