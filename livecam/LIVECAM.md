# :snake: Python Script LiveCAM

Ce script est utilisé pour récupérer et analyser les images envoyées par votre / vos caméra(s) de surveillance (par exemple)
afin de vérifier si des personnes sont présentes sur la photo et déclencher un autre script personnalisé si c'est le cas.

### :gear: Setup
Installez la librairie [requests](https://requests.readthedocs.io/en/master/)
```
pip install requests
```

Pour utiliser le script, créez un fichier nommé `.auth` dans le même répertoire que le script avec le contenu suivant:
```
mail:[Votre adresse e-mail]
password:[Votre mot de passe]
server:[Le serveur IMAP à utiliser]
port:[Le port du serveur]
```
Remplissez les champs avec les informations nécessaires.

*Exemple de serveur IMAP:*<br>
Gmail: `imap.gmail.com` / 993<br>
Outlook: `outlook.office365.com` / 993<br>
Yahoo: `imap.mail.yahoo.com` / 993<br>

**Paramètres du script:**<br>
Dans le fichier `livecam.conf`:
```
PAUSE = 60                          # Temps entre chaque exécution en secondes
DAYS_BEFORE_DELETION = 14           # Nombre de jour avant suppression d'une vieille photo
PATH_FOR_PICTURES = "pictures/"     # Chemin où les photos seront stockés
API_URL = "http://127.0.0.1:8080"   # URL de l'API
ALIAS = "RPI"                       # Alias donné aux objets détectés
CLASS = "person"                    # Classe d'objet à détecter
CONFIDENCE = 80                     # Taux de confiance minimum pour valider l'objet détecté
HOOK = "hook.py"                    # Script python à exécuter en cas de personne détectée
```

### :rocket: Lancer le script
```
python3 livecam.py
```
