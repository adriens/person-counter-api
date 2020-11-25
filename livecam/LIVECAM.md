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
PAUSE = 60                          # Time between script execution (SECONDS)
DAYS_BEFORE_DELETION = 14           # Number of days before old photos are deleted
PATH_FOR_PICTURES = "pictures/"     # Absolute path where the pictures will be stored
API_URL = "http://127.0.0.1:8080"   # URL for API calls
ALIAS = "RPI"                       # Alias given to objects detected
CLASS = "person"                    # Filter for class of objects
CONFIDENCE = 80                     # Minimum confidence needed to be counted as a detected object
HOOK = "hook.py"                    # Python script to execute in case of detected person
```

### :rocket: Lancer le script
```
python3 livecam.py
```
