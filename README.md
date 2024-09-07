### spring auth template

> Still in development.

## How to use

Just create a project from this template. You can use the mode of `User` to add custom things to your user. Otherwise
just use spring as usual. A user service, controller and auth are already implemented.

## Needs
- postgres db (`docker-compose.yml`)
- java version 21

## Setup
- If no profile exists in your ide (if you are using intellij) create a new one which is running the `SpringAuthTemplateApplication` class.
- In dev make sure to start it in `dev` mode by selecting the environment in your run configuration.
- In prod make sure to activate `prod` as environment.