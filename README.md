# Solvle - A Word Puzzle Analysis Tool

Try it out at http://solvle.appsoil.com

This is a toy project that I made to learn React. It's the first React app I've written, 
and its initial shell borrows heavily from the Wordle-Clone-React tutorial, available on github:
https://github.com/machadop1407/Wordle-Clone-React

The backend for this application uses Spring Boot. It would probably have made sense to in-line
the analysis logic into the frontend and just download a dictionary to each user, but I
wanted to learn how to make a React app integrate with a Spring Boot controller, so this design choice
was strictly for educational value.

## Running the Application

The easiest way to run this application is through Docker. You should be able to clone this repo and run `docker-compose up`
from the parent directory to start an instance of both the front and backend locally.

If you prefer, you can start the apps outside of docker using your tools of choice. The back-end requires Java 18 or 
higher and the front-end uses node. Run the SolvleApplication class to launch the backend application and run `npm start` from the
/solvle-front directory.

## Using the Application
The design is intended to be familiar to users of other similar word guessing games.
1. Type letters or tap the on-screen keyboard to enter a word.
2. Click the letters you've entered to mark them gray, yellow, or green (unavailable, wrong position, or correct).
3. Press ENTER to advance the word choice to the next line.

The application will calculate optimal word choices based on the number of viable remaining words that
contain the letters of each word suggestion. It also displays the number of viable words containing
each letter of the alphabet as small numbers below the keyboard letters.

Lastly, the app calculates 'Fishing' words, which are words that may not be viable words, but contain
the highest frequency of *new* letters found in the remaining word options. This can be useful if
you don't think you can correctly guess on the next word and want to maximize the amount of 
information gained.

## Dictionaries
There are a variety of word lists included:

* The Default 2315 wordlist that used to be the standard solution set.
* An expanded wordlist of 3158 words that was briefly exposed on the NYT website.
* A reduced list that for awhile I was manually removing used words from (not up to date any more)
* A larger list of 14856 that may or may not be the full list of viable words you can type in.

There are also a couple foreign language word lists, populated from this site:
https://github.com/titoBouzout/Dictionaries
