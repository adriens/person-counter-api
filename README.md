# API de détection d'objets et de comptage de personnes :man:

API utilisant la librairie de Machine Learning DJL ainsi que TensorFlow afin de dÃ©tecter les objets sur une image.<br>
Les objets détectés peuvent ainsi être comptÃés.
  
L'API renvoie les données sous format Json.

Lien vers [DJL](https://djl.ai/) <br>
Lien vers [TensorFlow](https://www.tensorflow.org/)

## :whale: Docker

L'api est disponible sur [Dockerhub](https://hub.docker.com/r/rastadidi/person-counter-api)

## :mailbox: Endpoints
```
/                         #Â AccÃ¨s Ã  la documentation
/photos/list              # Permet de rÃ©cupÃ©rer la liste des images disponible
/photos/{img}/detect      # Permet de rÃ©cupÃ©rer les informations sur les objets dÃ©tectÃ©s
/photos/{img}/visualize   # Permet de lancer l'affichage des objets dÃ©tectÃ©es sur une image
/photos/{img}/metadata    # Permet de rÃ©cupÃ©rer les mÃ©ta-donnÃ©es d'une image
/photos/{img}/analysis    # Permet de rÃ©cupÃ©rer les donnÃ©es liÃ© Ã  l'exÃ©cution de l'application
/photos/{img}/detect/full # Combine les rÃ©sultats de /detect et /metadata
```

#### Endpoints pour les images hÃ©bergÃ©es par des sites tierces
```
/photos/thirdparty/{host}/{file}/detect         #Â Permet de rÃ©cupÃ©rer les informations sur les objets dÃ©tectÃ©s sur une image externe
/photos/thirdparty/{host}/{file}/visualize      #Â Permet de lancer l'affichage des objets dÃ©tectÃ©es sur une image externe
/photos/thirdparty/{host}/{file}/metadata       # Permet de rÃ©cupÃ©rer les mÃ©ta-donnÃ©es d'une image externe
/photos/thirdparty/{host}/{file}/analysis       # Permet de rÃ©cupÃ©rer les donnÃ©es liÃ© Ã  l'exÃ©cution de l'application sur une image externe
/photos/thirdparty/{host}/{file}/detect/full    # Combine les rÃ©sultats de /detect et /metadata pour une image externe
```
`{file}` : RÃ©cuperez l'identifiant de l'image en copiant l'adresse de celle-ci.<br>
`{host}` : RÃ©cupÃ©rez le nom du site dans la liste ci-dessous.<br>

Liste des sites tierces supportÃ©s pour les images externes:<br>
[imgur](https://imgur.com/)

#### ParamÃ¨tres optionnels pour les endpoints
```
?class=[className]      # Permet de filtrer les rÃ©sultats sur une classe d'objets
?confidence=[50-100]    # Permet de fitlrer les rÃ©sultats sur un taux de probabilitÃ© minimum
?alias=[alias]          # Ajoute un alias aux objets retournÃ©s
```

Exemple:<br>
`/photos/{img}/detect?class=person&confidence=90` retourne les personnes dÃ©tectÃ©es avec un taux de probabilitÃ© de 90% minimum.

## :gear: DÃ©marrer l'API
### Par l'image Docker
**Important**: Vous devez dÃ©finir un rÃ©pertoire qui sera le point de montage entre votre machine et l'image Docker et qui contiendra les images d'entrÃ©es de l'API.<br>
Pour se faire, utilisez l'option `-v` lors du `docker run`<br>
Exemple: ``docker run -v ~/Images:/input gbertherat/person-counter-api:latest`` partagera votre rÃ©pertoire "Images" avec l'image Docker.
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

## :gear: DÃ©marrer le monitoring ELK
```
cd person-counter-api
docker-compose -f elk.yml up -d
```

### AccÃ©der au monitoring ELK
<http://0.0.0.0:5601>

### Charger le dashboard
Se rendre Ã  l'adresse <http://0.0.0.0:5601/app/management/kibana/objects>
```
Importer le fichier "Dashboard.ndjson"
Puis se rendre dans l'onglet Dashboard et cliquer sur Person Counter Dashboard
```

### :memo: Note
Le script de simulation de camÃ©ra doit tourner en fond afin de rendre le monitoring dynamique
```
cd person-counter-api/demo
./camera-simulation.sh
```

# jib build

In your `~/.m2/settings.xml` put your Docker hub

```xml
<server>
    <id>registry.hub.docker.com</id>
    <username>rastadidi</username>
    <password>XXXXXXX</password>
</server>
```

Then build/push to DockerHub :

`mvn compile jib:build`
