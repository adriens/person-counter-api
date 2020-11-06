# API de détection d'objets et de comptage de personnes :man:

API utilisant la librairie de Machine Learning DJL ainsi que TensorFlow afin de détecter les objets sur une image.<br>
Les objets détectés peuvent ainsi être compté.
  
L'API renvoie les données sous format Json.

Lien vers [DJL](https://djl.ai/) <br>
Lien vers [TensorFlow](https://www.tensorflow.org/)

## :whale: Docker

L'api est disponible sur [Dockerhub](https://hub.docker.com/repository/docker/gbertherat/person-counter-api)

## :mailbox: Endpoints
```
/photos/list              # Permet de récupérer la liste des images disponible
/photos/{img}/add         # Permet d'ajouter une image à la liste des images disponibles, voir section Note ci-dessous
/photos/{img}/remove      # Permet de supprimer une image de la liste
/photos/{img}/detect      # Permet de récupérer les informations sur les objets détectés
/photos/{img}/visualize   # Permet de lancer l'affichage des objets détectées sur une image
/photos/{img}/metadata    # Permet de récupérer les méta-données d'une image
/photos/{img}/analysis    # Permet de récupérer les données lié à l'exécution de l'application
```

### :clipboard: Note
Pour ajouter une image, récupérer l'identifiant d'une image de <imgur.com>.<br>
Exemple:<br>
<https://imgur.com/67tSocD> --> Copier l'adresse de l'image --> https://i.imgur.com/67tSocD.jpeg<br>
L'identifiant c'est "67tSocD.jpeg"
```
/photos/67tSocD.jpeg/add  # Télécharge et ajoute l'image https://i.imgur.com/67tSocD.jpeg à la liste
```

## :gear: Démarrer l'image Docker
```
docker pull gbertherat/person-counter-api:latest
docker images
docker run -d -p 8080:8080 --name=person-counter-api gbertherat/person-counter-api:latest 
docker ps
```

## :gear: Démarrer le service
```
git clone https://github.com/adriens/person-counter-api
cd person-counter-api
./mvnw spring-boot:run
```


