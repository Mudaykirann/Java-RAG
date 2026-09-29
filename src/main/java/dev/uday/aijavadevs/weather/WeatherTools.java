package dev.uday.aijavadevs.weather;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class WeatherTools {

    public record WeatherResponse(String location, double temperature, String condition) {}

    // Inner records for Open-Meteo API responses
    private record GeocodingResponse(List<GeocodingResult> results) {}
    private record GeocodingResult(double latitude, double longitude, String name, String country) {}
    
    private record ForecastResponse(CurrentWeather current_weather) {}
    private record CurrentWeather(double temperature, int weathercode) {}

    @Tool(description = "Get the current live weather and temperature for a specific city or location")
    public WeatherResponse getCurrentWeather(
            @ToolParam(description = "The name of the city or location, e.g. London, Tokyo, Hyderabad, New York") String location) {
        
        System.out.println("🤖 [Tool Call] LLM requested weather for: " + location);
        RestClient restClient = RestClient.create();
        
        try {
            // 1. Geocode city name to lat/lon using Open-Meteo Geocoding API
            GeocodingResponse geo = restClient.get()
                    .uri("https://geocoding-api.open-meteo.com/v1/search?name={city}&count=1", location)
                    .retrieve()
                    .body(GeocodingResponse.class);
                    
            if (geo == null || geo.results() == null || geo.results().isEmpty()) {
                return new WeatherResponse(location, 0, "Location not found");
            }
            
            GeocodingResult geoResult = geo.results().get(0);
            
            // 2. Fetch current weather using lat/lon
            ForecastResponse forecast = restClient.get()
                    .uri("https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true", 
                            geoResult.latitude(), geoResult.longitude())
                    .retrieve()
                    .body(ForecastResponse.class);
                    
            if (forecast == null || forecast.current_weather() == null) {
                return new WeatherResponse(geoResult.name(), 0, "Weather data unavailable");
            }
            
            double temp = forecast.current_weather().temperature();
            int code = forecast.current_weather().weathercode();
            String condition = mapWeatherCode(code);
            
            System.out.println("✅ [Tool Result] Weather fetched: " + temp + "°C, " + condition);
            return new WeatherResponse(geoResult.name() + ", " + geoResult.country(), temp, condition);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching weather: " + e.getMessage());
            return new WeatherResponse(location, 0, "Error connecting to weather service");
        }
    }
    
    // WMO Weather interpretation codes
    private String mapWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Mainly clear, partly cloudy, and overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 95 -> "Thunderstorm";
            default -> "Unknown condition";
        };
    }
}
