package ru.practicum.analyzer.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.infra.mapper.EventSimilarityMapper;
import ru.practicum.analyzer.infra.model.EventSimilarity;
import ru.practicum.analyzer.infra.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class SimilarityServiceImpl implements SimilarityService {

    private final SimilarityRepository similarityRepository;

    @Override
    public void save(EventSimilarityAvro eventSimilarityAvro) {
        log.info("Сохранение сходства событий А {} и Б {}: {}",
                eventSimilarityAvro.getEventA(),
                eventSimilarityAvro.getEventB(),
                eventSimilarityAvro.getScore());

        EventSimilarity newSimilarity = EventSimilarityMapper.toEntity(eventSimilarityAvro);
        if (newSimilarity == null) {
            log.error("Не удалось конвертировать EventSimilarityAvro в Entity: {}", eventSimilarityAvro);
            return;
        }

        if (isSelfSimilarity(newSimilarity)) {
            log.warn("Попытка сохранить сходство события {} с самим собой. Пропускаем.",
                    newSimilarity.getEvent1());
            return;
        }

        similarityRepository
                .findByEvent1AndEvent2(newSimilarity.getEvent1(), newSimilarity.getEvent2())
                .ifPresentOrElse(
                        existing -> updateExistingSimilarity(existing, newSimilarity),
                        () -> createNewSimilarity(newSimilarity)
                );
    }

    private boolean isSelfSimilarity(EventSimilarity similarity) {
        return similarity.getEvent1().equals(similarity.getEvent2());
    }

    private void updateExistingSimilarity(EventSimilarity existing, EventSimilarity updated) {
        log.debug("Обновляем сходство событий {} и {}: {} -> {}",
                existing.getEvent1(),
                existing.getEvent2(),
                existing.getSimilarity(),
                updated.getSimilarity());

        existing.setSimilarity(updated.getSimilarity());
        existing.setTimestamp(updated.getTimestamp());
        similarityRepository.save(existing);
    }

    private void createNewSimilarity(EventSimilarity newSimilarity) {
        log.debug("Создаем новую связь между событиями {} и {} со сходством {}",
                newSimilarity.getEvent1(),
                newSimilarity.getEvent2(),
                newSimilarity.getSimilarity());

        similarityRepository.save(newSimilarity);
    }
}
