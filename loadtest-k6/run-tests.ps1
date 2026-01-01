$apps = @(
    @{ Name = "MVC (Blocking)"; Port = "8081"; Output = "results-mvc.json" },
    @{ Name = "WebFlux (Java)"; Port = "8082"; Output = "results-webflux-java.json" },
    @{ Name = "WebFlux (Kotlin)"; Port = "8083"; Output = "results-webflux-kotlin.json" }
)

foreach ($app in $apps) {
    Write-Host "Running Load Test for $($app.Name) on port $($app.Port)..."
    k6 run -e PORT=$($app.Port) --out json=$($app.Output) script.js
    Write-Host "Finished $($app.Name)"
    Start-Sleep -Seconds 5
}
