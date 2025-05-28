from django.db import models

class User(models.Model):
    first_name = models.CharField(max_length=100)
    last_name = models.CharField(max_length=100, blank=True, null=True)
    email = models.EmailField(unique=True)
    password = models.CharField(max_length=128)  # Esto debería contener un hash
    phone = models.CharField(max_length=20, blank=True, null=True)
    city = models.CharField(max_length=100, blank=True, null=True)
    username = models.CharField(max_length=100, unique=True)
    verification_token = models.CharField(max_length=100, blank=True, null=True)
    is_verified = models.BooleanField(default=False)

    class Meta:
        db_table = 'users'

    def __str__(self):
        return self.username

class Breed(models.Model):
    name = models.CharField(max_length=200, unique=True)
    max_temperature = models.FloatField()  # °C
    min_temperature = models.FloatField()
    max_heart_rate = models.IntegerField()  # lpm
    min_heart_rate = models.IntegerField()

    def __str__(self):
        return self.name

class Pet(models.Model):
    name = models.CharField(max_length=100)
    species = models.CharField(max_length=100)
    breed = models.ForeignKey(Breed, on_delete=models.SET_NULL, blank=True, null=True)
    birth_date = models.DateField(blank=True, null=True)
    created_at = models.DateTimeField(auto_now_add=True)
    owner = models.ForeignKey(User, on_delete=models.CASCADE, related_name='pets')

    class Meta:
        db_table = 'pets'

    def __str__(self):
        return self.name

class Alert(models.Model):
    pet = models.ForeignKey(Pet, on_delete=models.CASCADE, related_name='alerts')
    message = models.TextField()
    timestamp = models.DateTimeField(auto_now_add=True)
    severity = models.CharField(max_length=50, choices=[
        ('LOW', 'Low'),
        ('MEDIUM', 'Medium'),
        ('HIGH', 'High'),
    ])

    class Meta:
        db_table = 'alerts'

    def __str__(self):
        return f"Alert for {self.pet.name} - {self.severity}"