package dev.vk.bot.game;

import dev.vk.bot.entities.Game;
import dev.vk.bot.entities.Question;
import dev.vk.bot.entities.Users;
import dev.vk.bot.game.service.GameService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static dev.vk.bot.game.service.GameService.NO_ANSWER;
import static dev.vk.bot.game.service.GameService.QUESTION;

@Slf4j
@Data
public class GameSession extends Thread {

    private final GameService gameService;

    private Map<Users, Integer> scoreMap = new HashMap<>();
    private Map<Users, Integer> eloMap;

    private final Game game;
    private final int peerId;
    private final Iterable<Users> usersIterable;
    private AtomicInteger questionIterator;

    public GameSession(GameService gameService, Game game, int peerId, Iterable<Users> usersIterable) {
        this.gameService = gameService;
        this.game = game;
        this.peerId = peerId;
        this.usersIterable = usersIterable;
        this.questionIterator = new AtomicInteger(game.getQuestionIterator());
    }

    @Override
    public void run() {
        addUsersInScoreMap(usersIterable);
        int maxQuestionAmount = game.getMaxQuestion();
        while (questionIterator.get() < maxQuestionAmount) {
            try {
                Thread.sleep(1500);
                sendNextQuestion();
                Thread.sleep(25000);
            } catch (InterruptedException e) {
                continue;
            }
            gameService.getMessageSender().sendMessage(peerId, String.format(NO_ANSWER, game.getCurrentQuestion().getAnswer()));
        }
        gameService.stopGame(game, peerId);
    }

    public void sendNextQuestion() {
        game.setQuestionIterator(questionIterator.incrementAndGet());
        Question question = gameService.getQuestionRepo().getRandomQuestion();
        game.setCurrentQuestion(question);
        gameService.getMessageSender().sendMessage(peerId, String.format(QUESTION, question.getQuestion()));
        gameService.getGameRepo().save(game);
    }

    public void addUsersInScoreMap(Iterable<Users> usersIterable) {
        usersIterable.forEach(user -> scoreMap.put(user, 0));
    }

    public String getResult() {
        StringBuilder sb = new StringBuilder("--- Результаты ---\n");
        int counter = 1;
        scoreMap = scoreMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        eloMap = gameService.getEloForGame(scoreMap);
        for (Map.Entry<Users, Integer> result : scoreMap.entrySet()) {
            sb.append(counter++)
                    .append(". ")
                    .append(result.getKey().getName())
                    .append(" | ")
                    .append(eloMap.get(result.getKey()) >= 0 ? "➕" : "➖")
                    .append(Math.abs(eloMap.get(result.getKey())))
                    .append("\n");
        }
        return sb.toString();
    }
}
