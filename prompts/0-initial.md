We're in an empty project for a Java Spring Boot project.
I've added the dependency for the JDA Discord bot library.
I will describe what this project should do, basically all parts need to be implemented.

This is a dynamic QR code router. The basic use case is as follows:
- List of URL paths can be defined. Let's call it "Static URLs"
- The static URLs will be printed in various ways as QR codes
- This project will serve those URLs
- These URLs will be mapped to a dynamic component. So the printed URL will be redirected to the dynamic mapped URL
- A discord bot, using JDA, will be launched within this spring boot application that will be used to update the configured QR codes
- For now, let's create a slash command where you input the nickname of the QR code, and it updates the mapping.
- This implies we actually have three things: Nick name, Hosted QR URL, Dynamic redirect destination

Some more details:
- It's intended to be simple, so let's not use any database. Instead, we'll save a config file in YAML along side where ever the server jar starts up from. It will be manually deployed
- Let's use Jackson for reading/writing yaml

Documentation for JDA
GitHub: https://github.com/discord-jda/JDA
Wiki: https://jda.wiki/introduction/jda/
JavaDocs: https://docs.jda.wiki/index.html

I think the JavaDocs are also downloaded with the source? Can you access it?

Let's implement this.
