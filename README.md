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

#### Endpoints pour les images hébergées par des sites tierces
```
/photos/thirdparty/{host}/{file}/detect         # Permet de récupérer les informations sur les objets détectés sur une image externe
/photos/thirdparty/{host}/{file}/visualize      # Permet de lancer l'affichage des objets détectées sur une image externe
/photos/thirdparty/{host}/{file}/metadata       # Permet de récupérer les méta-données d'une image externe
/photos/thirdparty/{host}/{file}/analysis       # Permet de récupérer les données lié à l'exécution de l'application sur une image externe
/photos/thirdparty/{host}/{file}/detect/full    # Combine les résultats de /detect et /metadata pour une image externe
```
`{file}` : Récuperez l'identifiant de l'image en copiant l'adresse de celle-ci.<br>
`{host}` : Récupérez le nom du site dans la liste ci-dessous.<br>

Liste des sites tierces supportés pour les images externes:<br>
[imgur](https://imgur.com/)

#### Paramètres optionnels pour les endpoints
```
?class=[className]      # Permet de filtrer les résultats sur une classe d'objets
?confidence=[50-100]    # Permet de fitlrer les résultats sur un taux de probabilité minimum
?alias=[alias]          # Ajoute un alias aux objets retournés
```

Exemple:<br>
`/photos/{img}/detect?class=person&confidence=90` retourne les personnes détectées avec un taux de probabilité de 90% minimum.

## :gear: Démarrer l'API
### Par l'image Docker
**Important**: Vous devez définir un répertoire qui sera le point de montage entre votre machine et l'image Docker et qui contiendra les images d'entrées de l'API.<br>
Pour se faire, utilisez l'option `-v` lors du `docker run`<br>
Exemple: ``docker run -v ~/Images:/input gbertherat/person-counter-api:latest`` partagera votre répertoire "Images" avec l'image Docker.
```
docker pull gbertherat/person-counter-api:latest
docker images
docker run -v [Chemin/Vers/Votre/Dossier]:/input -d -p 8080:8080 --name=person-counter-api gbertherat/person-counter-api:latest 
docker ps
```

### Localement
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
Se rendre à l'adresse <http://0.0.0.0:5601/app/management/kibana/objects>
```
Importer le fichier "Dashboard.ndjson"
Puis se rendre dans l'onglet Dashboard et cliquer sur Person Counter Dashboard
```

### :memo: Note
Le script de simulation de caméra doit tourner en fond afin de rendre le monitoring dynamique
```
cd person-counter-api/demo
./camera-simulation.sh
```
