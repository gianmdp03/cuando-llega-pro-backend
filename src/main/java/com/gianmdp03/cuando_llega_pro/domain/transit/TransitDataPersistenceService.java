package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.Linea;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.Parada;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.ParadaLinea;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.ParadaLineaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns transactional writes to the normalized static transit catalogue. */
@Service
@RequiredArgsConstructor
public class TransitDataPersistenceService {

    private final EntityManager entityManager;
    private final ParadaLineaRepository paradaLineaRepository;

    @Transactional
    public void replaceEmptyCatalog(JsonNode dataset, int batchSize) {
        JsonNode lineas = dataset.path("lineas");
        if (!lineas.isArray()) {
            throw new IllegalArgumentException("El dataset no contiene el arreglo lineas");
        }
        for (JsonNode lineaNode : lineas) {
            upsertLinea(lineaNode.path("codigoLinea").asText(), lineaNode.path("nombre").asText());
        }
        entityManager.flush();

        JsonNode paradas = dataset.path("paradas");
        if (!paradas.isArray()) {
            throw new IllegalArgumentException("El dataset no contiene el arreglo paradas");
        }
        int processed = 0;
        for (JsonNode paradaNode : paradas) {
            persistParada(paradaNode);
            if (++processed % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
    }

    @Transactional
    public void synchronizeLineStops(Linea linea, JsonNode response) {
        JsonNode paradas = response.has("paradas") ? response.path("paradas") : response;
        if (!paradas.isArray()) {
            return;
        }
        for (JsonNode paradaNode : paradas) {
            String identificador = firstText(paradaNode, "Identificador", "identificador", "Codigo", "codigo");
            if (identificador == null) {
                continue;
            }

            Parada parada = entityManager.find(Parada.class, identificador);
            if (parada == null) {
                parada = new Parada(
                        identificador,
                        firstText(paradaNode, "Codigo", "codigo"),
                        firstText(paradaNode, "Descripcion", "descripcion"),
                        firstDouble(paradaNode, "LatitudParada", "latitud", "Latitud"),
                        firstDouble(paradaNode, "LongitudParada", "longitud", "Longitud")
                );
                entityManager.persist(parada);
            } else {
                updateParada(parada, paradaNode);
            }

            String bandera = firstText(paradaNode, "AbreviaturaBandera", "bandera");
            if (bandera != null && paradaLineaRepository
                    .findByParadaIdentificadorAndLineaCodigoAndBandera(identificador, linea.getCodigo(), bandera)
                    .isEmpty()) {
                parada.addLinea(new ParadaLinea(
                        entityManager.getReference(Linea.class, linea.getCodigo()),
                        bandera,
                        firstText(paradaNode, "AbreviaturaAmpliadaBandera", "banderaAmpliada")
                ));
            }
        }
    }

    private void persistParada(JsonNode paradaNode) {
        String identificador = requiredText(paradaNode, "identificador");
        Parada parada = new Parada(
                identificador,
                textOrNull(paradaNode, "codigo"),
                textOrNull(paradaNode, "descripcion"),
                doubleOrNull(paradaNode, "latitud"),
                doubleOrNull(paradaNode, "longitud")
        );
        for (JsonNode lineaNode : paradaNode.path("lineas")) {
            String codigoLinea = requiredText(lineaNode, "codigoLinea");
            parada.addLinea(new ParadaLinea(
                    entityManager.getReference(Linea.class, codigoLinea),
                    requiredText(lineaNode, "bandera"),
                    textOrNull(lineaNode, "banderaAmpliada")
            ));
        }
        entityManager.persist(parada);
    }

    private void upsertLinea(String codigo, String nombre) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Una línea del dataset no tiene codigoLinea");
        }
        Linea linea = entityManager.find(Linea.class, codigo);
        if (linea == null) {
            entityManager.persist(new Linea(codigo, nombre));
        } else {
            linea.setNombre(nombre);
        }
    }

    private void updateParada(Parada parada, JsonNode source) {
        String codigo = firstText(source, "Codigo", "codigo");
        String descripcion = firstText(source, "Descripcion", "descripcion");
        Double latitud = firstDouble(source, "LatitudParada", "latitud", "Latitud");
        Double longitud = firstDouble(source, "LongitudParada", "longitud", "Longitud");
        if (codigo != null) parada.setCodigo(codigo);
        if (descripcion != null) parada.setDescripcion(descripcion);
        if (latitud != null) parada.setLatitud(latitud);
        if (longitud != null) parada.setLongitud(longitud);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Campo obligatorio ausente: " + field);
        }
        return value;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = textOrNull(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static Double firstDouble(JsonNode node, String... fields) {
        for (String field : fields) {
            Double value = doubleOrNull(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }
}
