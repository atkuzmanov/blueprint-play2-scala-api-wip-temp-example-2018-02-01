@Status
Feature: Example status endpoint health checks

  Scenario: Status health checks
    When I request the status of the example test http/https rest api
    Then the example test http/https rest api response status code is 200
    And the example test http/https rest api response has Content-Type header values "charset=utf-8" and "application/json"
