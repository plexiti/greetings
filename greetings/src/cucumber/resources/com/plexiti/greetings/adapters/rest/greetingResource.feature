Feature: Get greeting

  As a consumer of the greetings resource
  I should be able to get a greeting

  Scenario Outline: Get greeting for caller

    Given I use the caller <caller>
    When I request a greeting
    Then I should get a response with HTTP status code <status>
    And The response should contain the message <message>
    And A greeting with the <message> should be stored

    Examples:
      | caller | status | message            |
      | Duke   | 200    | Hello World, Duke! |
      | Tux    | 200    | Hello World, Tux!  |

  Scenario: Get greeting using caller 0xCOFFEEPOT

    Given I use the caller 0xCOFFEEPOT
    When I request a greeting
    Then I should get a response with HTTP status code 418
