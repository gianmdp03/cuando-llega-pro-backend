package com.gianmdp03.cuando_llega_pro.domain.preset;

import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "presets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
public class Preset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "codigo_linea", nullable = false)
    @ToString.Include
    private String codigoLinea;

    @Column(name = "identificador_parada", nullable = false)
    @ToString.Include
    private String identificadorParada;

    @Column(name = "bandera")
    @ToString.Include
    private String bandera;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private PresetConfig config;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Preset(String codigoLinea, String identificadorParada, String bandera) {
        this.codigoLinea = codigoLinea;
        this.identificadorParada = identificadorParada;
        this.bandera = bandera;
        this.createdAt = Instant.now();
    }

    public Preset(User user, String codigoLinea, String identificadorParada, String bandera) {
        this.user = user;
        this.codigoLinea = codigoLinea;
        this.identificadorParada = identificadorParada;
        this.bandera = bandera;
        this.createdAt = Instant.now();
    }

    public Preset(User user, String codigoLinea, String identificadorParada, String bandera, PresetConfig config) {
        this.user = user;
        this.codigoLinea = codigoLinea;
        this.identificadorParada = identificadorParada;
        this.bandera = bandera;
        this.config = config;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Preset other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
