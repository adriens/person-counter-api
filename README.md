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
/                         # Accès à la documentation
/photos/list              # Permet de récupérer la liste des images disponible
/photos/{img}/detect      # Permet de récupérer les informations sur les objets détectés
/photos/{img}/visualize   # Permet de lancer l'affichage des objets détectées sur une image
/photos/{img}/metadata    # Permet de récupérer les méta-données d'une image
/photos/{img}/analysis    # Permet de récupérer les données lié à l'exécution de l'application
/photos/{img}/detect/full # Combine les résultats de /detect et /metadata
```

## :gear: Démarrer l'image Docker
**Important**: Vous devez définir un répertoire qui sera le point de montage entre votre machine et l'image Docker et qui contiendra les images d'entrées de l'API.<br>
Pour se faire, utilisez l'option `-v` lors du `docker run`<br>
Exemple: ``docker run -v ~/Images:/input gbertherat/person-counter-api:latest`` partagera votre répertoire "Images" avec l'image Docker.
```
docker pull gbertherat/person-counter-api:latest
docker images
docker run -v [Chemin/Vers/Votre/Dossier]:/input -d -p 8080:8080 --name=person-counter-api gbertherat/person-counter-api:latest 
docker ps
```

## :gear: Démarrer le service
```
git clone https://github.com/adriens/person-counter-api
cd person-counter-api
./mvnw spring-boot:run
```


