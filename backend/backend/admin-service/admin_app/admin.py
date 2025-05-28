from django.contrib import admin
from .models import User, Breed, Pet

@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ('username', 'email', 'first_name', 'last_name', 'city')
    search_fields = ('username', 'email', 'first_name', 'last_name')
    list_filter = ('city',)
    ordering = ('-id',)

@admin.register(Breed)
class BreedAdmin(admin.ModelAdmin):
    list_display = ('name', 'min_temperature', 'max_temperature', 'min_heart_rate', 'max_heart_rate')
    search_fields = ('name',)
    ordering = ('name',)

@admin.register(Pet)
class PetAdmin(admin.ModelAdmin):
    list_display = ('name', 'species', 'breed', 'owner', 'created_at')
    search_fields = ('name', 'owner__username', 'species')
    list_filter = ('species',)
    ordering = ('-created_at',)