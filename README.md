# Getting Started

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

