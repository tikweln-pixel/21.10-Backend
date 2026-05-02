package com.votify.dto;

/**
 * Representa un comentario de la comunidad (voto popular o jurado)
 * ya clasificado por el backend como positivo o sugerencia de mejora.
 *
 * - autor:    nombre del votante o "Participante" si es anónimo.
 * - texto:    contenido del comentario.
 * - esMejora: true si el texto contiene señales de crítica constructiva
 *             (keywords: "pero", "falta", "mejorable", etc.).
 *             false si el comentario es positivo o neutral.
 */
public record ComentarioAnalizadoDto(String autor, String texto, boolean esMejora) {}
