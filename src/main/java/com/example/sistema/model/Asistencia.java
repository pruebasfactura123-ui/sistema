package com.example.sistema.model;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Entity
@Table(name = "asistencias")
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", nullable = false)
    private Usuario usuario; 

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada", nullable = true)
    private LocalTime horaEntrada;

    @Column(name = "hora_salida", nullable = true)
    private LocalTime horaSalida;

    @Column(name = "estado", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'ASISTENCIA'")
    private String estado = "ASISTENCIA";

    @Column(name = "observaciones", nullable = true, length = 255)
    private String observaciones;

    public Asistencia() {}

    // ==========================================
    // MÉTODOS AUXILIARES PARA AGRUPACIÓN SEMANAL
    // ==========================================
    public String getEtiquetaSemana() {
        if (fecha == null) return "Sin fecha";
        LocalDate inicioSemana = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int numSemana = fecha.get(weekFields.weekOfWeekBasedYear());
        
        return "Semana " + numSemana + " (" + inicioSemana.format(fmt) + " - " + finSemana.format(fmt) + ")";
    }

    public LocalDate getInicioSemana() {
        if (fecha == null) return LocalDate.MIN;
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime horaEntrada) { this.horaEntrada = horaEntrada; }

    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}