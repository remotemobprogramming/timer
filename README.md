# timer

The timer.mob.sh service for the mob.sh tool.

## Credits

Created by [Dr. Simon Harrer](https://twitter.com/simonharrer) and [Jochen Christ](https://twitter.com/jochen_christ) in Oktober 2021.

Currently maintained by [Gregor Riegler](https://twitter.com/gregor_riegler) and [Joshua Töpfer](https://twitter.com/JoshuaToepfer), and to some limited degree still by [Dr. Simon Harrer](https://twitter.com/simonharrer).


### droplet install

- droplet with docker
- fixed ip
- firewall

```bash
# in droplet once
git clone --recurse-submodules https://github.com/evertramos/nginx-proxy-automation.git webproxy
cd webproxy/bin && ./fresh-start.sh --yes -e simon.harer@gmail.com --skip-docker-image-check

# in dev
./build

# in droplet
docker stop mobtimer
docker rm mobtimer
docker run -d -e VIRTUAL_HOST=timer.mob.sh -e LETSENCRYPT_HOST=timer.mob.sh -e LETSENCRYPT_EMAIL=simon.harrer@gmail.com -e PORT=80 --expose 80 --network=proxy --name mobtimer simonharrer/mob-timer:latest
echo "done"
```

