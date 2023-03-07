package com.ruslanburduzhan.catsbot.service;

import com.ruslanburduzhan.catsbot.entity.Filter;
import com.ruslanburduzhan.catsbot.entity.Telegrambot;
import com.ruslanburduzhan.catsbot.entity.User;
import com.ruslanburduzhan.catsbot.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
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
        menuMap.put("jpg", "Дай изображение \uD83C\uDFB4");
        menuMap.put("gif", "Дай гифку \uD83C\uDFAC");
        menuMap.put("settings", "Настройки ⚙️");
        menuMap.put("text", "Текст \uD83D\uDDFF");
        menuMap.put("filter", "Фильтр  \uD83D\uDC7D");
        menuMap.put("scheduler", "Расписание \uD83D\uDD52");
        menuMap.put("reset", "Сброс \uD83D\uDC8A");
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
                queue.clear();
            } else if (msgText.equals(menuMap.get("text"))) {
                queue.add("text");
                String text = userRepository.findById(chatId).get().getText();
                if (text == null)
                    sendMessage(chatId, "Текущий текст не введён.");
                else
                    sendMessage(chatId, "Текущий текст - " + text);
                sendMessage(chatId, "Введите текст, который отобразиться на картинке/гифке:");
            } else if (msgText.equals(menuMap.get("reset"))) {
                chooseResetOptions(chatId);
            } else if (msgText.equals(menuMap.get("filter"))) {
                chooseFilterOptions(chatId);
            } else if (Objects.equals(queue.peek(), "text")) {
                enterText(chatId, msgText);
            }
            if (!msgText.equals(menuMap.get("text")))
                queue.clear();
        } else if (update.hasCallbackQuery()) { // **************** CALL_BACK_QUERY ****************
            String callBackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int msgId = update.getCallbackQuery().getMessage().getMessageId();

            if (callBackData.startsWith("filter_")) {
                filterCallBackQueryHandler(chatId, msgId, callBackData.substring(7));
            } else {
                resetCallBackQueryHandler(chatId, msgId, callBackData);
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

    private void sendEditeMsg(long chatId, int msgId, String text) {
        EditMessageText msg = EditMessageText.builder().chatId(chatId)
                .messageId(msgId).text(text).build();
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhotoMsg(long chatId) {
        String text = "";
        User user = userRepository.findById(chatId).get();
        if (user.getText() != null) {
            text = "/says/" + userRepository.findTextByChatId(chatId).get().replace(" ", "%20");
        }
        if (user.getFilter() != null) {
            text += "?filter=" + user.getFilter().toLowerCase();
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
        String text = "";
        User user = userRepository.findById(chatId).get();
        if (user.getText() != null) {
            text = "/says/" + userRepository.findTextByChatId(chatId).get().replace(" ", "%20");
        }
        if (user.getFilter() != null) {
            text += "?filter=" + user.getFilter().toLowerCase();
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

    private void enterText(long chatId, String msgText) {
        boolean flag = true;
        for (Map.Entry<String, String> entry : menuMap.entrySet())
            if (entry.getKey().equals(msgText)) {
                flag = false;
                break;
            }
        if (flag) {
            User user = userRepository.findById(chatId).get();
            user.setText(msgText);
            userRepository.save(user);
            sendMessage(chatId, "Задан текст - " + msgText);
        }

    }

    private void chooseResetOptions(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Что нужно сбросить?");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        var btn1 = InlineKeyboardButton.builder().text("Текст").callbackData("text").build();
        rowInLine.add(btn1);
        var btn2 = InlineKeyboardButton.builder().text("Фильтр").callbackData("filter").build();
        rowInLine.add(btn2);
        rowsInLine.add(rowInLine);

        var btn3 = InlineKeyboardButton.builder().text("Расписание").callbackData("scheduler").build();
        rowInLine2.add(btn3);
        rowsInLine.add(rowInLine2);

        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void chooseFilterOptions(long chatId) {
        String text = userRepository.findById(chatId).get().getFilter();
        if (text != null)
            sendMessage(chatId, "Текущий фильтр - " + text);
        else
            sendMessage(chatId, "Текущий фильтр не выбран.");
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите фильтр:");
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        var btn1 = InlineKeyboardButton.builder().text("PAINT").callbackData("filter_PAINT").build();
        rowInLine.add(btn1);
        var btn2 = InlineKeyboardButton.builder().text("MONO").callbackData("filter_MONO").build();
        rowInLine.add(btn2);
        rowsInLine.add(rowInLine);

        var btn3 = InlineKeyboardButton.builder().text("SEPIA").callbackData("filter_SEPIA").build();
        rowInLine2.add(btn3);
        var btn4 = InlineKeyboardButton.builder().text("NEGATIVE").callbackData("filter_NEGATIVE").build();
        rowInLine2.add(btn4);
        rowsInLine.add(rowInLine2);

        markup.setKeyboard(rowsInLine);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
        row.add(menuMap.get("filter"));
        keyboardRowList.add(row);
        row2.add(menuMap.get("scheduler"));
        keyboardRowList.add(row2);
        row3.add(menuMap.get("reset"));
        keyboardRowList.add(row3);
        row4.add(menuMap.get("back"));
        keyboardRowList.add(row4);
        keyboardMarkup.setKeyboard(keyboardRowList);
        sendMessageWithKeyboardMarkup(chatId, "Настройки:", keyboardMarkup);
    }

    private void resetCallBackQueryHandler(long chatId, int msgId, String callBackData) {
        sendEditeMsg(chatId, msgId, "Что нужно сбросить?");
        User user = userRepository.findById(chatId).get();
        if (callBackData.equals("text")) {
            user.setText(null);
            sendMessage(chatId, "Текст сброшен.");
        } else if (callBackData.equals("filter")) {
            user.setFilter(null);
            sendMessage(chatId, "Фильтр сброшен.");
        } else if (callBackData.equals("scheduler")) {
            user.setSchedulerTime(null);
            sendMessage(chatId, "Расписание сброшено.");
        }
        userRepository.save(user);

    }

    private void filterCallBackQueryHandler(long chatId, int msgId, String callBackData) {
        // PAINT MONO SEPIA NEGATIVE
        sendEditeMsg(chatId, msgId, "Выберите фильтр:");
        User user = userRepository.findById(chatId).get();
        user.setFilter(callBackData);
        userRepository.save(user);
        sendMessage(chatId, "Выбран фильтр - " + callBackData);
    }



}
