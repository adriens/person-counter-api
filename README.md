[![Build Status](https://travis-ci.org/adriens/person-counter-api.svg?branch=main)](https://travis-ci.org/adriens/person-counter-api)

# API de détection d'objets et de comptage de personnes :man:

API utilisant la librairie de Machine Learning DJL ainsi que TensorFlow afin de détecter les objets
sur une image.
Les objets détectés peuvent ainsi être comptés.
  
L'API renvoie les données sous format Json.

- [DJL](https://djl.ai/)
- [TensorFlow](https://www.tensorflow.org/)

## :whale: Docker

L'api est disponible sur [Dockerhub](https://hub.docker.com/r/rastadidi/person-counter-api)

## :mailbox: Endpoints
```
/                         # Accès à la documentation
/photos/list              # Permet de récupérer la liste des images disponible
/photos/{img}/detect      # Permet de récupérer les informations sur les objets détectés
/photos/{img}/visualize   # Permet de lancer l'affichage des objets détectés sur une image
/photos/{img}/metadata    # Permet de récupérer les méta-données d'une image
/photos/{img}/analysis    # Permet de récupérer les données liées à  l'exécution de l'application
/photos/{img}/detect/full # Combine les résultats de /detect et /metadata
```

#### Endpoint pour les requêtes RAW avec CURL ou HTTPIE
```
curl --data-binary @image.jpg "127.0.0.1:8080/photos/raw"
http -v ':8080/photos/raw' < image.jpg 
```
*Note*: Les paramètres optionnels fonctionnent aussi pour ce endpoint.

#### Endpoints pour les images hébergées par des sites tiers
```
/photos/thirdparty/{host}/{file}/detect         # Permet de récupérer les informations sur les objets détectés sur une image externe
/photos/thirdparty/{host}/{file}/visualize      # Permet de lancer l'affichage des objets détectés sur une image externe
/photos/thirdparty/{host}/{file}/metadata       # Permet de récupérer les méta-données d'une image externe
/photos/thirdparty/{host}/{file}/analysis       # Permet de récupérer les données liées à l'exécution de l'application sur une image externe
/photos/thirdparty/{host}/{file}/detect/full    # Combine les résultats de /detect et /metadata pour une image externe
```

- `{file}` : Récupérer l'identifiant de l'image en copiant l'adresse de celle-ci
- `{host}` : Récupérer le nom du site dans la liste ci-dessous

Sites tiers supportés pour les images externes:
- [imgur](https://imgur.com/)

*Exemple*: <br>
Trouver une image sur imgur: https://imgur.com/67tSocD <br>
---> Clique droit sur l'image <br>
---> Copier l'adresse de l'image <br>
---> https://i.imgur.com/67tSocD.jpg <br>
---> Copier l'identifiant 
---> 67tSocD.jpg
- `/photos/thirdparty/imgur/67tSocD.jpg/detect` lance la détection sur l'image 67tSocD.jpg disponible sur imgur

#### Paramètres optionnels pour les endpoints

```
?class=[className]      # Permet de filtrer les résultats sur une classe d'objets (ex: `person`)
?confidence=[50-100]    # Permet de fitlrer les résultats sur un taux de probabilité minimum (ex: `80`)
?alias=[alias]          # Ajoute un alias aux objets retournés
```

*Exemple*:
- `/photos/{img}/detect?class=person&confidence=90` retourne les personnes détectées avec un taux de probabilité de 90% minimum.

## :gear: Démarrer l'API
### Par l'image Docker
**Important**: Vous devez définir un répertoire qui sera le point de montage entre votre machine et l'image Docker
et qui contiendra les images d'entrées de l'API.

Pour se faire, utilisez l'option `-v` lors du `docker run`

*Exemple*: <br>
``docker run -v ~/Images:/input rastadidi/person-counter-api:latest`` partagera votre répertoire "Images" avec l'image Docker.
```
docker pull rastadidi/person-counter-api:latest
docker images
docker run -v [Chemin/Vers/Votre/Dossier]:/input -d -p 8080:8080 --name=person-counter-api rastadidi/person-counter-api:latest 
docker ps
```

### Localement

Run from source:

```
git clone https://github.com/adriens/person-counter-api
cd person-counter-api
./mvnw spring-boot:run
```

## :gear: Démarrer le monitoring ELK

```
cd person-counter-api
docker-compose -f elk.yml up -d
```

### Accéder au monitoring ELK

<http://0.0.0.0:5601>

### Charger le dashboard

Se rendre à  l'adresse <http://0.0.0.0:5601/app/management/kibana/objects>

```
Importer le fichier "Dashboard.ndjson"
Puis se rendre dans l'onglet Dashboard et cliquer sur Person Counter Dashboard
```

### :memo: Note


Le script de simulation de caméra doit tourner en tâche de fond afin de rendre le monitoring dynamique

```
cd person-counter-api/demo
./camera-simulation.sh
```

# Build JIB

Dans votre `~/.m2/settings.xml`, placez vos identifiants DockerHub:
```xml
<server>
    <id>registry.hub.docker.com</id>
    <username>username</username>
    <password>password</password>
</server>
```
Et build & push vers DockerHub
`mvn compile jib:build`

