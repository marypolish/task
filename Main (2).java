package samples;//Варіант 2

//Підрахунок слів у тексті
//        Створіть ConcurrentHashMap, де ключами є слова, а значеннями — їх
//        частота появи. Розділіть текст на кілька частин і використовуйте Callable, щоб
//        кожна частина підраховувала частоту слів.
//        Використовуйте Future, щоб зібрати та вивести результати.
//        У задачі необхідно використати метод isDone().

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Набір слів
        String text = "я я я Java багатопотоковість забезпечує ефективну обробку даних. Обробка даних важлива для ефективної роботи програм. Java є однією з найпопулярніших мов програмування";

        // Видаляємо розділові знаки і переводимо весь текст у нижній регістр
        text = text.replaceAll("[\\p{Punct}]", "").toLowerCase();

        // Розбиваємо текст на масив слів
        String[] words = text.split("\\s+");

        // ConcurrentHashMap - для підрахнку слів (ключі слова, значення частота появи)
        // AtomicInteger - для потокобезпечного збільшення лічильника (лічильник збільшується, помилки не виникають)
        ConcurrentHashMap<String, AtomicInteger> wordCountMap = new ConcurrentHashMap<>();

        // Розбиття масиву слів на частини для обробки в різних потоках (2 частини), один потік одна частина
        List<String[]> parts = splitArray(words, 3);

        // Створюємо ExecutorService з кількістю потоків, рівною кількості частин
        // (всі потоки відразу в одному ExecutorService, щоб з ними всіма працювати одночасно)
        ExecutorService executorService = Executors.newFixedThreadPool(parts.size());

        // Створюємо список для зберігання Future об'єктів (список слів які він знайшов)
        List<Future<Void>> futures = new ArrayList<>();

        // Для кожної частини створюємо задачу (Callable - підраховує слова в певному потоці), яка обробляє свою частину масиву
        for (String[] part : parts) {
            Callable<Void> task = () -> {
                // Отримання імені потоку, який виконує цю задачу
                String threadName = Thread.currentThread().getName();
                System.out.println("Потік: " + threadName + "розпочато");

                for (String word : part) {
                    wordCountMap.computeIfAbsent(word.toLowerCase(), k -> new AtomicInteger(0)).incrementAndGet();
                }
                return null;
            };

            // Додаємо задачу до списку задач для виконання
            futures.add(executorService.submit(task));
        }


        // Очікуємо завершення всіх задач за допомогою Future
        for (Future<Void> future : futures) {
            while (!future.isDone()) {
                System.err.println("Задача ще виконується...");
                Thread.sleep(100); // Пауза між перевірками
            }
        }

        // Після завершення всіх задач виводимо результати підрахунку слів
        System.err.println("Підрахунок слів завершено:");
        wordCountMap.forEach((word, count) -> System.out.println(word + ": " + count));

        // Закриваємо ExecutorService після завершення всіх задач
        executorService.shutdown();
    }

    // Метод для розбивання масиву слів на певну кількість частин
    private static List<String[]> splitArray(String[] array, int parts) {
        List<String[]> result = new ArrayList<>();
        int length = array.length;
        // Визначаємо розмір кожної частини (chunk), враховуючи кількість слів і кількість частин
        // (ділить наш текст на три рівні частини)
        int chunkSize = (length + parts - 1) / parts;

        // Копіюємо частини з оригінального масиву в нові підмасиви
        for (int i = 0; i < length; i += chunkSize) {
            result.add(Arrays.copyOfRange(array, i, Math.min(length, i + chunkSize)));
        }
        return result;
    }
}

