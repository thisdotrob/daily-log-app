# Daily Log App

## Development
Start the server compiler, repl and service:
```
clojure -A:client-server
(start-dev)
```

Install deps and start the client compiler & repl:
```
npm i
npx webpack
clojure -A:client-dev
```

The app will be available at `http://localhost:8890/index.html`

and the test runner at `http://localhost:9500/figwheel-extra-main/auto-testing`.
