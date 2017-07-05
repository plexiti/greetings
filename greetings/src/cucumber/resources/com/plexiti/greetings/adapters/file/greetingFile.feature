Feature: Store greeting in hot folder

  As a user of the greetings hot folder
  I should be able to place a greeting command

  Scenario Outline: Place greeting for caller

    Given I use the caller <caller>
    When I place a greeting in the hot folder
    Then A command with the caller <caller> should be stored

    Examples:
      | caller   |
      | Martin   |
      | Peter    |
