# Daily Log App

## Development
Start the server compiler, repl and service:
```
clojure -A:server-dev
(start-dev)
```

Install deps and start the client compiler & repl:
```
npm i
npx webpack
clojure -A:client-dev
```

Your browser should then open the app, at `http://localhost:8890/index.html`.

The test runner can be found  at
`http://localhost:9500/figwheel-extra-main/auto-testing`.
