package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  UserService userService;
  @Mock
  NotificationService notificationService;
  @InjectMocks
  LibraryManager libraryManager;

  @BeforeEach
  void setUp() {

  }

  @ParameterizedTest
  @CsvSource({
      "Book1, 13",
      "TomasA, 1",
      "Kniga, 1"
  })
  void testAddBookAndGetAvailableCopiesThisBook(String bookName, int quantity) {
    libraryManager.addBook(bookName, quantity);
    int countBook1 = libraryManager.getAvailableCopies(bookName);
    assertEquals(quantity, countBook1);
  }


  @Test
  void testBorrowBookWithNotActiveAccount() {
    when(userService.isUserActive(any())).thenReturn(false);
    libraryManager.addBook("Book1", 1);
    boolean bookIsBorrow = libraryManager.borrowBook("Book1", "Иван");

    assertAll("Проверка, что метод borrowBook вернул false и правильно сработал notificationService.notifyUser())",
        () -> assertFalse(bookIsBorrow),
        () -> verify(notificationService,
            times(1))
            .notifyUser("Иван", "Your account is not active."));
  }
  @Test
  void testBorrowBookWithNotAvailableCopies() {
    when(userService.isUserActive(any())).thenReturn(true);

    assertAll("Проверка, что метод borrowBook вернул false и ни разу не сработал notificationService.notifyUser())",
        () ->  assertFalse(libraryManager.borrowBook("Book1", "Иван")),
        () -> verify(notificationService, times(0))
            .notifyUser(any(),any()));

  }
  @Test
  void testBorrowBook() {
    when(userService.isUserActive(any())).thenReturn(true);
    libraryManager.addBook("Book1", 1);

    assertAll("Проверка, что метод borrowBook вернул true и правильно сработал notificationService.notifyUser())",
        () -> assertEquals(1,libraryManager.getAvailableCopies("Book1")),
        () ->  assertTrue(libraryManager.borrowBook("Book1", "Иван")),
        () -> assertEquals(0,libraryManager.getAvailableCopies("Book1")),
        () -> verify(notificationService, times(1))
            .notifyUser("Иван", "You have borrowed the book: Book1"));
  }

  @Test
  void testReturnBookWithEmptyBorrowedBook() {
    //Не добавляем ничего в borrowedBooks и пытаемся веогуть книгу.
    assertFalse(libraryManager.returnBook("Book1", "Иван"));
  }
  @Test
  void testReturnBookByAnotherUser() {
    //Добавляем книгу в borrowedBooks и берем её от имени Иван, а пытаемся вернуть от имени Петя.
    when(userService.isUserActive(any())).thenReturn(true);
    libraryManager.addBook("Book1", 1);
    assertAll("Проверка, что книга выдается Ивану, но не выдается Пете",
        () -> assertTrue(libraryManager.borrowBook("Book1", "Иван")),
        () -> assertFalse(libraryManager.returnBook("Book1", "Петя")));
  }
  @Test
  void testReturnBook() {
    //Добавляем книгу в borrowedBooks, берем её от имени Иван, возвращаем от имени Иван и проверяем работу notifyUser.
    when(userService.isUserActive(any())).thenReturn(true);
    libraryManager.addBook("Book1", 1);
    assertAll("Добавляем книгу в borrowedBooks, берем её от имени Иван, возвращаем от имени Иван и проверяем что работу notifyUser",
        () -> assertTrue(libraryManager.borrowBook("Book1", "Иван")),
        () -> assertTrue(libraryManager.returnBook("Book1", "Иван")),
        () -> assertEquals(1, libraryManager.getAvailableCopies("Book1")),
        () -> verify(notificationService, times(1))
            .notifyUser("Иван","You have returned the book: Book1"));
  }

  @Test
  void testGetAvailableCopies(){
    assertEquals(0, libraryManager.getAvailableCopies("Book1"));
    libraryManager.addBook("Book1", 13);
    assertEquals(13, libraryManager.getAvailableCopies("Book1"));
  }

  //Проверка работы метода при подаче в него отрицательного overdueDays
  @Test
  void testCalculateDynamicLateFee_WithOverdueDaysLessThatZero(){
    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-15, true,true));

    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  // Проверка работы метода с разными входными данными
  @ParameterizedTest
  @CsvSource({
      "7.2, 12, true, true",
      "234.6, 391, true, true",
      "4.8, 12, false, true",
      "9.0, 12, true, false",
  })
  void testCalculateDynamicLateFee(double expectedResult,
                                   int overdueDays,
                                   boolean isBestseller,
                                   boolean isPremiumMember) {
    assertEquals(expectedResult,
        libraryManager.calculateDynamicLateFee(overdueDays, isBestseller,isPremiumMember));
  }
}