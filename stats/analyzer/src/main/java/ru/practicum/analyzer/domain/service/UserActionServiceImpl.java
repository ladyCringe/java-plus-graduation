package ru.practicum.analyzer.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.infra.mapper.UserActionMapper;
import ru.practicum.analyzer.infra.model.UserAction;
import ru.practicum.analyzer.infra.repository.UserInteractionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class UserActionServiceImpl implements UserActionService {

    private final UserInteractionRepository userInteractionRepository;

    @Override
    public void save(UserActionAvro userActionAvro) {
        log.info("Сохраняем действие {} пользователя с id {} для события c id {}",
                userActionAvro.getActionType(),
                userActionAvro.getUserId(),
                userActionAvro.getEventId());

        UserAction newUserAction = UserActionMapper.toEntity(userActionAvro);

        userInteractionRepository
                .findByUserIdAndEventId(newUserAction.getUserId(), newUserAction.getEventId())
                .ifPresentOrElse(
                        existing -> updateExistingAction(existing, newUserAction),
                        () -> createNewAction(newUserAction)
                );
    }

    private void updateExistingAction(UserAction existingAction, UserAction newAction) {
        if (newAction.getRating() > existingAction.getRating()) {
            log.debug("Обновляем рейтинг для пользователя {} и события {}: {} -> {}",
                    existingAction.getUserId(),
                    existingAction.getEventId(),
                    existingAction.getRating(),
                    newAction.getRating());

            existingAction.setRating(newAction.getRating());
            existingAction.setTimestamp(newAction.getTimestamp());
            userInteractionRepository.save(existingAction);
            return;
        }

        log.debug("Рейтинг не обновлен: текущий {} >= нового {} для пользователя {} и события {}",
                existingAction.getRating(),
                newAction.getRating(),
                existingAction.getUserId(),
                existingAction.getEventId());
    }

    private void createNewAction(UserAction newAction) {
        log.debug("Создаем новую запись взаимодействия для пользователя {} и события {} с рейтингом {}",
                newAction.getUserId(),
                newAction.getEventId(),
                newAction.getRating());

        userInteractionRepository.save(newAction);
    }
}
