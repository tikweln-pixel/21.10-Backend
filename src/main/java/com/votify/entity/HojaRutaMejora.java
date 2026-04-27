package com.votify.entity;

import jakarta.persistence.*;
import java.util.Date;

/**
 * Entidad que almacena la hoja de ruta de mejora personalizada de un competidor.
 *
 * En la implementación parcial (sin IA), el resumen se genera automáticamente
 * a partir de los comentarios recibidos en EvaluacionComentario.
 * Cuando se integre IA, generadoIa = true y resumenGenerado contendrá el texto
 * producido por el modelo.
 *
 * Una hoja de ruta es única por competidor + categoría.
 */
@Entity
@Table(
    name = "hoja_ruta_mejora",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_hoja_ruta_competitor_category",
        columnNames = {"competitor_id", "category_id"}
    )
)
public class HojaRutaMejora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competitor_id", nullable = false)
    private Competitor competitor;

    /**
     * Categoría a la que pertenece esta hoja de ruta.
     * Nullable para permitir hojas de ruta globales (todas las categorías).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    /**
     * Resumen textual generado (automático o por IA).
     * Contiene un párrafo de síntesis de los comentarios recibidos.
     */
    @Column(name = "resumen_generado", columnDefinition = "TEXT")
    private String resumenGenerado;

    /**
     * true si el resumen fue generado por IA; false si es automático (parcial).
     */
    @Column(name = "generado_ia", nullable = false)
    private boolean generadoIa = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_generacion", nullable = false)
    private Date fechaGeneracion;

    public HojaRutaMejora() {
    }

    public HojaRutaMejora(Competitor competitor, Category category,
                           String resumenGenerado, boolean generadoIa) {
        this.competitor = competitor;
        this.category = category;
        this.resumenGenerado = resumenGenerado;
        this.generadoIa = generadoIa;
        this.fechaGeneracion = new Date();
    }

    // Getters y Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Competitor getCompetitor() { return competitor; }
    public void setCompetitor(Competitor competitor) { this.competitor = competitor; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getResumenGenerado() { return resumenGenerado; }
    public void setResumenGenerado(String resumenGenerado) { this.resumenGenerado = resumenGenerado; }

    public boolean isGeneradoIa() { return generadoIa; }
    public void setGeneradoIa(boolean generadoIa) { this.generadoIa = generadoIa; }

    public Date getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(Date fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
}
