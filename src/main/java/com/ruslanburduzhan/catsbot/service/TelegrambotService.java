package com.ruslanburduzhan.catsbot.service;

import com.ruslanburduzhan.catsbot.entity.Telegrambot;
import com.ruslanburduzhan.catsbot.entity.User;
import com.ruslanburduzhan.catsbot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Service
public class TelegrambotService extends TelegramLongPollingBot {

    private Telegrambot telegrambot;
    private CatService catService;
    private UserRepository userRepository;
    private Map<String, String> menuMap;
    private Queue<String> queue;

    public TelegrambotService(Telegrambot telegrambot, CatService catService, UserRepository userRepository) {
        this.telegrambot = telegrambot;
        this.catService = catService;
        this.userRepository = userRepository;
        menuMap = new HashMap<>();
        queue = new ArrayDeque<>();
        createCommandList();
        createMenuMap();
    }

    private void createCommandList() {
        List<BotCommand> botCommandList = new ArrayList<>();
//        botCommandList.add(new BotCommand("/start", "Начало работы"));
        botCommandList.add(new BotCommand("/menu", "Показать меню"));
        try {
            execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void createMenuMap() {
        menuMap.put("jpg", "Дай изображение");
        menuMap.put("gif", "Дай гифку");
        menuMap.put("settings", "Настройки");
        menuMap.put("text", "Текст");
        menuMap.put("type", "Формат");
        menuMap.put("scheduler", "Расписание");
        menuMap.put("filter", "Фильтр");
        menuMap.put("reset", "Сбросить настройки?");
        menuMap.put("back", "Назад ⬅️");
    }

    @Override
    public String getBotUsername() {
        return telegrambot.getName();
    }

    @Override
    public String getBotToken() {
        return telegrambot.getKey();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();
            String msgText = msg.getText();
            long chatId = msg.getChatId();
            if (msgText.equals("/start")) {
                registerUser(msg);
            } else if (msgText.equals("/menu") || msgText.equals(menuMap.get("back"))) {
                showMenu(chatId);
            } else if (msgText.equals(menuMap.get("settings"))) {
                showSettings(chatId);
            } else if (msgText.equals(menuMap.get("jpg"))) {
                sendPhotoMsg(chatId);
            } else if (msgText.equals(menuMap.get("gif"))) {
                sendAnimationMsg(chatId);
            } else if (msgText.equals(menuMap.get("text"))) {
                queue.add("text");
                sendMessage(chatId,"Введите текст, который отобразиться на картинке/гифке:");
            } else if (Objects.equals(queue.peek(), "text")){
                User user = userRepository.findById(chatId).get();
                user.setText(msgText);
                userRepository.save(user);
            }
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setParseMode(ParseMode.HTML);
        message.setChatId(chatId);
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPhotoMsg(long chatId) {
        String text = null;
        if(userRepository.findTextByChatId(chatId).isPresent()){
            text = "/says/" + userRepository.findTextByChatId(chatId).get().replace(" ","%20");
        }
        SendPhoto sendPhoto = SendPhoto.builder().chatId(chatId)
                .photo(new InputFile(catService.getCatImage(text))).build();
        try {
            execute(sendPhoto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAnimationMsg(long chatId) {
        String text = null;
        if(userRepository.findTextByChatId(chatId).isPresent()){

            text = "/says/" + userRepository.findTextByChatId(chatId).get().replace(" ","%20");
        }
        SendAnimation animation = SendAnimation.builder().chatId(chatId).
                animation(new InputFile(catService.getCatImage("/gif" + text))).build();
        try {
            execute(animation);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessageWithKeyboardMarkup(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        keyboardMarkup.setResizeKeyboard(true);
        SendMessage message = new SendMessage();
        message.setReplyMarkup(keyboardMarkup);
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void registerUser(Message msg) {
        long chatId = msg.getChatId();
        if (userRepository.findById(chatId).isEmpty()) {
            userRepository.save(new User(chatId));
        }
    }


    private void showMenu(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        row.add(menuMap.get("jpg"));
        row.add(menuMap.get("gif"));
        keyboardRowList.add(row);
        row2.add(menuMap.get("settings"));
        keyboardRowList.add(row2);
        keyboardMarkup.setKeyboard(keyboardRowList);
        sendMessageWithKeyboardMarkup(chatId, "Меню:", keyboardMarkup);
    }

    private void showSettings(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();
        row.add(menuMap.get("text"));
        row.add(menuMap.get("type"));
        keyboardRowList.add(row);
        row2.add(menuMap.get("scheduler"));
        row2.add(menuMap.get("filter"));
        keyboardRowList.add(row2);
        row3.add(menuMap.get("reset"));
        keyboardRowList.add(row3);
        row4.add(menuMap.get("back"));
        keyboardRowList.add(row4);
        keyboardMarkup.setKeyboard(keyboardRowList);
        sendMessageWithKeyboardMarkup(chatId, "Настройки:", keyboardMarkup);
    }


}
